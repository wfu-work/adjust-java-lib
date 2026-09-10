import com.google.gson.*;
import com.navfirst.adjust.lib.domains.*;
import com.navfirst.adjust.lib.domains.Geometry.*;
import com.navfirst.adjust.lib.services.*;
import com.navfirst.adjust.lib.services.impl.*;
import com.navfirst.adjust.lib.configurations.*;
import com.navfirst.adjust.lib.exceptions.*;
import com.navfirst.adjust.lib.utils.*;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Bounded report experiments. Every check has an assertion; no license/data needed. */
public class AdjustReportProbe {
  static final Gson G = new GsonBuilder().setPrettyPrinting().create();
  static final AdjustLibService SDK = new AdjustLibServiceImpl();
  static final List<Map<String,Object>> checks = new ArrayList<>();
  static final Map<String,Object> results = new LinkedHashMap<>();
  interface Test { void run() throws Exception; }
  static void test(String id, Test t) {
    long s = System.nanoTime(); Map<String,Object> row = new LinkedHashMap<>(); row.put("id",id);
    try { t.run(); row.put("passed",true); }
    catch(Throwable e) { row.put("passed",false); row.put("error",e.toString()); }
    row.put("ms",(System.nanoTime()-s)/1e6); checks.add(row);
  }
  static void require(boolean b,String m) { if(!b) throw new AssertionError(m); }
  static void near(double a,double b,double tol) { require(Double.isFinite(a)&&Math.abs(a-b)<=tol,"actual="+a+" expected="+b+" tol="+tol); }
  static double[] xyz(ENU p) { return new double[]{p.getEast(),p.getNorth(),p.getUp()}; }
  static void vector(ENU p,double[] v,double tol) { double[] a=xyz(p);for(int i=0;i<3;i++) near(a[i],v[i],tol); }
  static void nominalQuality(NetworkDiagnostics d,List<String> warnings) {
    require(d.isConverged()&&d.isStationCovarianceAvailable()&&d.isResidualDiagnosticsAvailable(),"quality availability/convergence");
    require(d.getGlobalTest()!=null&&d.getGlobalTest().isPassed(),"nominal global test must pass");
    near(d.getGlobalTest().getConfidence(),.95,1e-12);
    require(warnings==null||warnings.isEmpty(),"unexpected nominal warnings: "+warnings);
  }
  static AdjustException expect(String code,Test t) throws Exception {
    try { t.run(); } catch(AdjustException e) { require(code.equals(e.getCode()),"Expected "+code+" got "+e.getCode()+" "+e.getMessage());return e; }
    throw new AssertionError("Expected "+code);
  }
  static ENUNetworkProblem weighted() {
    return ENUNetworkProblem.builder().stations(List.of(ENUNetworkProblem.Station.builder().id("A").fixed(true).knownENU(new ENU(0,0,0)).build(),ENUNetworkProblem.Station.builder().id("B").build()))
      .baselines(new ArrayList<>(List.of(baseline("AB1","A","B",new ENU(10,2,.5),Matrix3.fromStdDev(1,1,1)),baseline("AB2","A","B",new ENU(12,4,1.5),Matrix3.fromStdDev(2,2,2))))).build();
  }
  static ENUNetworkProblem.Baseline baseline(String id,String a,String b,ENU p,Matrix3 q) {return ENUNetworkProblem.Baseline.builder().id(id).from(a).to(b).vector(p).covariance(q).build();}
  static GeodeticNetworkProblem geodetic() {
    WGS84Coordinate o=new WGS84Coordinate(30.5,114.3,50);
    return GeodeticNetworkProblem.builder().stations(List.of(GeodeticNetworkProblem.Station.builder().id("A").exactWGS84(o).build(),GeodeticNetworkProblem.Station.builder().id("B").build()))
      .baselines(List.of(GeodeticNetworkProblem.Baseline.builder().id("AB1").from("A").to("B").frameWGS84(o).vectorENU(new ENU(10,2,.5)).covarianceENU(Matrix3.fromStdDev(.01,.01,.02)).build(),GeodeticNetworkProblem.Baseline.builder().id("AB2").from("A").to("B").frameWGS84(o).vectorENU(new ENU(10.01,2.01,.52)).covarianceENU(Matrix3.fromStdDev(.01,.01,.02)).build())).build();
  }
  static ENUNetworkProblem network(int n) {
    List<ENUNetworkProblem.Station> stations=new ArrayList<>();List<ENUNetworkProblem.Baseline> baselines=new ArrayList<>();
    stations.add(ENUNetworkProblem.Station.builder().id("S0").fixed(true).knownENU(new ENU(0,0,0)).build());
    for(int i=1;i<n;i++) {
      stations.add(ENUNetworkProblem.Station.builder().id("S"+i).build());
      for(int j=0;j<2;j++) {double sign=(j==0?1:-1)/Math.sqrt(2);baselines.add(baseline("L"+i+"_"+j,"S0","S"+i,new ENU(i*10+sign*.02,i*2+sign*.02,i*.5+sign*.03),Matrix3.fromStdDev(.02,.02,.03)));}
    }
    return ENUNetworkProblem.builder().stations(stations).baselines(baselines).build();
  }
  public static void main(String[] args) throws Exception {
    Path out=Path.of(args[0]);JsonObject fixture=JsonParser.parseString(Files.readString(Path.of(args[1]))).getAsJsonObject();
    results.put("native_version",SDK.getVersion()); results.put("java_version",System.getProperty("java.version"));
    test("weighted_analytic",()->{
      ENUNetworkResult r=SDK.startAdjust(weighted());results.put("weighted",r);
      vector(r.getStations().get(1).getPosition(),new double[]{10.4,2.4,.7},1e-11);
      vector(r.getStations().get(1).getStdDev(),new double[]{Math.sqrt(.48),Math.sqrt(.48),Math.sqrt(.48)},1e-11);
      near(r.getDiagnostics().getSigma0(),Math.sqrt(.6),1e-11); require(r.getDiagnostics().getDegreesOfFreedom()==3,"dof");
      vector(r.getBaselines().get(0).getResidual(),new double[]{-.4,-.4,-.2},1e-11);
      nominalQuality(r.getDiagnostics(),r.getWarnings());
    });
    test("correlated_loop_numpy_oracle",()->{
      ENUNetworkProblem p=G.fromJson(fixture.get("problem"),ENUNetworkProblem.class);ENUNetworkResult r=SDK.startAdjust(p);results.put("correlated_loop",r);JsonObject o=fixture.getAsJsonObject("oracle");double max=0;
      for(int i=0;i<3;i++) {double[] expected=G.fromJson(o.getAsJsonArray("coordinates").get(i),double[].class);vector(r.getStations().get(i+1).getPosition(),expected,1e-9);vector(r.getStations().get(i+1).getStdDev(),G.fromJson(o.getAsJsonArray("stddev").get(i),double[].class),1e-9);for(int j=0;j<3;j++)max=Math.max(max,Math.abs(xyz(r.getStations().get(i+1).getPosition())[j]-expected[j]));}
      for(int i=0;i<7;i++)vector(r.getBaselines().get(i).getResidual(),G.fromJson(o.getAsJsonArray("residuals").get(i),double[].class),1e-9);
      near(r.getDiagnostics().getSigma0(),o.get("sigma0").getAsDouble(),1e-9);require(r.getDiagnostics().getDegreesOfFreedom()==o.get("dof").getAsInt(),"dof");results.put("oracle_max_coordinate_error_m",max);
      nominalQuality(r.getDiagnostics(),r.getWarnings());
    });
    test("wgs84_ecef_independent_rotation",()->{
      GeodeticNetworkResult r=SDK.startAdjustWgs84(geodetic());results.put("wgs84",r);double lat=Math.toRadians(30.5),lon=Math.toRadians(114.3),a=6378137.,f=1/298.257223563,e2=f*(2-f),N=a/Math.sqrt(1-e2*Math.sin(lat)*Math.sin(lat));
      double[] origin={(N+50)*Math.cos(lat)*Math.cos(lon),(N+50)*Math.cos(lat)*Math.sin(lon),(N*(1-e2)+50)*Math.sin(lat)};
      double E=10.005,n=2.005,u=.51;double[] expected={origin[0]-Math.sin(lon)*E-Math.sin(lat)*Math.cos(lon)*n+Math.cos(lat)*Math.cos(lon)*u,origin[1]+Math.cos(lon)*E-Math.sin(lat)*Math.sin(lon)*n+Math.cos(lat)*Math.sin(lon)*u,origin[2]+Math.cos(lat)*n+Math.sin(lat)*u};
      ECEFCoordinate p=r.getStations().get(1).getPositionECEF();near(p.getX(),expected[0],1e-6);near(p.getY(),expected[1],1e-6);near(p.getZ(),expected[2],1e-6);
      GeodeticNetworkResult.Station b=r.getStations().get(1);ECEFCoordinate s=b.getStdDevXYZ();near(b.getMXYZ(),Math.sqrt(s.getX()*s.getX()+s.getY()*s.getY()+s.getZ()*s.getZ()),1e-12);require(b.isWgs84Absolute(),"absolute frame");
      vector(r.getBaselines().get(0).getResidualENU(),new double[]{-.005,-.005,-.01},1e-7); results.put("wgs84_expected_ecef",expected);
      nominalQuality(r.getDiagnostics(),r.getWarnings());
    });
    test("typed_vs_full_json_enu",()->{
      ENUNetworkProblem p=weighted();ENUNetworkResult a=SDK.startAdjust(p);JsonObject q=JsonParser.parseString(JsonUtils.toRequestJson(p,null)).getAsJsonObject();q.getAsJsonObject("options").addProperty("covariance","full");String raw=SDK.startAdjust(q.toString());JsonObject rr=JsonUtils.parseResponse(raw).result();results.put("full_response_fields",rr.keySet());
      ENUNetworkResult b=JsonUtils.decode(raw,ENUNetworkResult.class);require(G.toJsonTree(a).equals(G.toJsonTree(b)),"typed vs full projection differs");require(rr.has("covariance"),"full covariance missing");
    });
    test("typed_vs_full_json_wgs84",()->{
      GeodeticNetworkProblem p=geodetic();GeodeticNetworkResult a=SDK.startAdjustWgs84(p);JsonObject q=JsonParser.parseString(JsonUtils.toRequestJson(p,null)).getAsJsonObject();q.getAsJsonObject("options").addProperty("covariance","full");GeodeticNetworkResult b=JsonUtils.decode(SDK.startAdjustWgs84(q.toString()),GeodeticNetworkResult.class);require(G.toJsonTree(a).equals(G.toJsonTree(b)),"typed/full WGS84 differs");
    });
    test("robust_outlier",()->{
      ENUNetworkProblem p=weighted();List<ENUNetworkProblem.Baseline> bs=new ArrayList<>();for(int i=0;i<10;i++){double e=(i%2==0?1:-1)*.01;bs.add(baseline("G"+i,"A","B",new ENU(10+e,2-e,.5+e),Matrix3.fromStdDev(.02,.02,.02)));}bs.add(baseline("OUT","A","B",new ENU(10.2,2.2,.7),Matrix3.fromStdDev(.02,.02,.02)));p.setBaselines(bs);
      ENUNetworkResult plain=SDK.startAdjust(p),robust=SDK.startAdjust(p,AdjustOptions.builder().robust(true).build());results.put("robust",Map.of("input",p,"target_m",new double[]{10,2,.5},"plain",plain,"huber",robust));
      require(plain.getDiagnostics().getGlobalTest()!=null&&!plain.getDiagnostics().getGlobalTest().isPassed(),"injected outlier must trigger ordinary global test");
      require(robust.getDiagnostics().isConverged()&&robust.getDiagnostics().isStationCovarianceAvailable(),"robust must converge with precision available");
      double weight=robust.getBaselines().get(10).getWeight();require(weight>.05&&weight<.25,"outlier weight outside expected interval");
      double[] target={10,2,.5},rp=xyz(robust.getStations().get(1).getPosition()),pp=xyz(plain.getStations().get(1).getPosition());
      for(int axis=0;axis<3;axis++){require(Math.abs(rp[axis]-target[axis])<.005,"robust axis bias exceeds 5 mm");require(Math.abs(rp[axis]-target[axis])<.5*Math.abs(pp[axis]-target[axis]),"robust must halve every axis bias");}
      for(int i=0;i<10;i++)near(robust.getBaselines().get(i).getWeight(),1,1e-12);
      require(robust.getDiagnostics().getGlobalTest()==null,"global test after downweight");
      require(List.of("global chi-square test is omitted after data-dependent robust reweighting").equals(robust.getWarnings()),"unexpected robust warnings: "+robust.getWarnings());
    });
    test("zero_redundancy_diagnostics",()->{ENUNetworkProblem p=weighted();p.setBaselines(List.of(p.getBaselines().get(0)));ENUNetworkResult r=SDK.startAdjust(p);results.put("zero_redundancy",r);require(r.getDiagnostics().getDegreesOfFreedom()==0&&r.getDiagnostics().getGlobalTest()==null,"zero dof global test");});
    test("core_option_mapping",()->{JsonObject q=JsonParser.parseString(JsonUtils.toRequestJson(weighted(),AdjustOptions.builder().robust(true).timeoutMs(5000L).build())).getAsJsonObject();JsonObject o=q.getAsJsonObject("options");require(q.get("timeout_ms").getAsLong()==5000&&o.getAsJsonObject("robust").size()==0&&o.get("covariance").getAsString().equals("station-blocks")&&o.get("datum").getAsString().equals("external")&&o.get("covariance_policy").getAsString().equals("required")&&o.getAsJsonObject("solver").get("method").getAsString().equals("dense"),"mapping");});
    test("null_and_default_options",()->{require(JsonUtils.toRequestJson(weighted(),null).equals(JsonUtils.toRequestJson(weighted(),new AdjustOptions())),"defaults");});
    test("timeout_bounds",()->{for(long v:new long[]{0,9223372036854L})JsonUtils.toRequestJson(weighted(),AdjustOptions.builder().timeoutMs(v).build());for(long v:new long[]{-1,9223372036855L})expect("invalid_request",()->JsonUtils.toRequestJson(weighted(),AdjustOptions.builder().timeoutMs(v).build()));});
    test("null_problem",()->expect("invalid_request",()->SDK.startAdjust((ENUNetworkProblem)null)));
    test("empty_json",()->{for(String s:new String[]{null,"","  "})expect("invalid_request",()->SDK.startAdjust(s));});
    test("nul_and_surrogate_rejected",()->{for(String s:new String[]{"{}\u0000","\uD800","\uDC00"})expect("invalid_request",()->JsonUtils.validateInput(s));JsonUtils.validateInput("{\"id\":\"测站😀\"}");});
    test("utf8_request_limit",()->{String limit="a".repeat(64*1024*1024);JsonUtils.validateInput(limit);expect("request_too_large",()->JsonUtils.validateInput(limit+"a"));expect("request_too_large",()->JsonUtils.validateInput("中".repeat(64*1024*1024/3+1)));});
    test("nonfinite_serialization",()->{for(double d:new double[]{Double.NaN,Double.POSITIVE_INFINITY})expect("invalid_request",()->JsonUtils.toJson(new ENU(d,0,0)));});
    test("matrix_defensive_copy",()->{double[] d={1,0,0,0,2,0,0,0,3};Matrix3 q=new Matrix3(d);d[0]=8;near(q.at(0,0),1,0);double[] b=q.getData();b[0]=9;near(q.at(0,0),1,0);try{new Matrix3(new double[8]);throw new AssertionError("bad length");}catch(IllegalArgumentException ok){};try{q.at(3,0);throw new AssertionError("bad index");}catch(IndexOutOfBoundsException ok){};near(Matrix3.fromStdDev(2,3,4).at(2,2),16,0);});
    test("native_invalid_json_raw_envelope",()->{String raw=SDK.startAdjust("{");results.put("invalid_json",raw);expect("invalid_json",()->JsonUtils.parseResponse(raw));});
    test("missing_covariance",()->{ENUNetworkProblem p=weighted();p.getBaselines().get(0).setCovariance(null);AdjustException e=expect("invalid_covariance",()->SDK.startAdjust(p));results.put("missing_covariance",Map.of("code",e.getCode(),"field",String.valueOf(e.getField()),"id",String.valueOf(e.getId())));});
    test("zero_covariance",()->{ENUNetworkProblem p=weighted();p.getBaselines().get(0).setCovariance(Matrix3.fromStdDev(0,0,0));expect("invalid_covariance",()->SDK.startAdjust(p));});
    test("asymmetric_covariance",()->{ENUNetworkProblem p=weighted();p.getBaselines().get(0).setCovariance(new Matrix3(new double[]{1,.2,0,0,1,0,0,0,1}));expect("invalid_covariance",()->SDK.startAdjust(p));});
    test("unknown_station",()->{ENUNetworkProblem p=weighted();p.getBaselines().get(0).setTo("Z");expect("unknown_station",()->SDK.startAdjust(p));});
    test("duplicate_station",()->{ENUNetworkProblem p=weighted();p.setStations(List.of(p.getStations().get(0),p.getStations().get(0)));expect("duplicate_station",()->SDK.startAdjust(p));});
    test("unanchored_network",()->{ENUNetworkProblem p=weighted();p.getStations().get(0).setFixed(false);p.getStations().get(0).setKnownENU(null);try{SDK.startAdjust(p);throw new AssertionError("unanchored network accepted");}catch(AdjustException e){results.put("unanchored_error",Map.of("code",e.getCode(),"field",String.valueOf(e.getField())));}});
    test("missing_wgs84_frame",()->{GeodeticNetworkProblem p=geodetic();p.getBaselines().forEach(b->b.setFrameWGS84(null));try{SDK.startAdjustWgs84(p);throw new AssertionError("missing frame accepted");}catch(AdjustException e){results.put("missing_frame_error",Map.of("code",e.getCode(),"field",String.valueOf(e.getField())));}});
    test("invalid_response_contract",()->{for(String s:new String[]{null,"[]","{}","{\"ok\":1}","{\"ok\":true}","{\"ok\":false}","{\"ok\":true,\"result\":{},\"error\":{}}","{\"ok\":true,\"result\":{}} trailing"})expect("invalid_response",()->JsonUtils.parseResponse(s));});
    test("business_error_fields",()->{AdjustException e=expect("example",()->JsonUtils.parseResponse("{\"ok\":false,\"error\":{\"code\":\"example\",\"message\":\"bad\",\"field\":\"stations\",\"id\":\"A\"}}"));require("stations".equals(e.getField())&&"A".equals(e.getId()),"error fields");});
    test("spring_auto_configuration",()->new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(AdjustAutoConfiguration.class)).run(c->{require(c.getStartupFailure()==null&&c.getBeansOfType(AdjustLibService.class).size()==1,"default bean");}));
    test("spring_disabled",()->new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(AdjustAutoConfiguration.class)).withPropertyValues("adjust.enabled=false").run(c->require(c.getBeansOfType(AdjustLibService.class).isEmpty(),"disabled bean")));
    test("spring_custom_bean",()->new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(AdjustAutoConfiguration.class)).withBean(AdjustLibService.class,()->SDK).run(c->require(c.getBeansOfType(AdjustLibService.class).size()==1&&c.getBean(AdjustLibService.class)==SDK,"custom bean")));
    test("concurrent_4_threads_80_calls",()->{
      String expected=G.toJson(SDK.startAdjust(weighted()));ExecutorService pool=Executors.newFixedThreadPool(4);long s=System.nanoTime();try{List<Future<String>> fs=new ArrayList<>();for(int i=0;i<80;i++)fs.add(pool.submit(()->G.toJson(SDK.startAdjust(weighted()))));for(Future<String> f:fs)require(expected.equals(f.get(30,TimeUnit.SECONDS)),"concurrent output differs");}finally{pool.shutdownNow();}results.put("concurrent",Map.of("threads",4,"calls",80,"batch_ms",(System.nanoTime()-s)/1e6));
    });
    test("repeatability_20_calls",()->{String expected=G.toJson(SDK.startAdjust(weighted()));for(int i=0;i<20;i++)require(expected.equals(G.toJson(SDK.startAdjust(weighted()))),"repeated output differs");});
    List<Map<String,Object>> perf=new ArrayList<>();
    for(int n:new int[]{10,50,150})test("performance_"+n+"_stations",()->{
      ENUNetworkProblem p=network(n);String q=JsonUtils.toRequestJson(p,null);List<Double> times=new ArrayList<>();double first=0;
      NetworkDiagnostics last=null;
      for(int i=0;i<18;i++){long s=System.nanoTime();ENUNetworkResult r=SDK.startAdjust(p);double ms=(System.nanoTime()-s)/1e6;if(i==0)first=ms;if(i>=3)times.add(ms);require(r.getStations().size()==n&&r.getBaselines().size()==2*(n-1),"scale result");nominalQuality(r.getDiagnostics(),r.getWarnings());near(r.getDiagnostics().getSigma0(),1,1e-9);for(int k=1;k<n;k++)vector(r.getStations().get(k).getPosition(),new double[]{k*10.,k*2.,k*.5},1e-8);last=r.getDiagnostics();}
      List<Double> sorted=new ArrayList<>(times);Collections.sort(sorted);Map<String,Object> row=new LinkedHashMap<>();row.put("stations",n);row.put("baselines",2*(n-1));row.put("parameters",3*(n-1));row.put("utf8_request_bytes",q.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);row.put("first_ms",first);row.put("warmup_calls",3);row.put("measured_ms",times);row.put("median_ms",sorted.get(7));row.put("p95_nearest_rank_ms",sorted.get(14));row.put("diagnostics",last);perf.add(row);
    });
    results.put("performance",perf);results.put("checks",checks);long passed=checks.stream().filter(x->Boolean.TRUE.equals(x.get("passed"))).count();results.put("passed",passed);results.put("failed",checks.size()-passed);
    Files.writeString(out,G.toJson(results)+"\n");System.out.println("checks="+checks.size()+" passed="+passed+" failed="+(checks.size()-passed));for(var c:checks)if(Boolean.FALSE.equals(c.get("passed")))System.out.println(c);
    if(passed!=checks.size())System.exit(1);
  }
}
