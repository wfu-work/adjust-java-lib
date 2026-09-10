#!/usr/bin/env python3
"""Run report experiments in a source copy and retain their evidence."""
import argparse
import datetime
import hashlib
import json
import os
import platform
from pathlib import Path
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET
from zipfile import ZipFile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument('--output', type=Path, required=True, help='New, empty output directory; existing evidence is never overwritten')
    args = parser.parse_args()
    source, output = args.source.resolve(), args.output.resolve()
    if output.exists() and any(output.iterdir()):
        parser.error('--output must be new or empty')
    output.mkdir(parents=True, exist_ok=True)
    work = output/'source-copy'
    work.mkdir()
    selected = []
    for entry in ['src', '.mvn', 'pom.xml', 'mvnw', 'mvnw.cmd', 'README.md', '.gitignore', '.gitattributes', 'scripts/build-native.sh']:
        path = source/entry
        if path.is_dir():
            selected.extend(p for p in path.rglob('*') if p.is_file() and p.name != '.DS_Store')
        elif path.is_file():
            selected.append(path)
    manifest = {'time':datetime.datetime.now().astimezone().isoformat(), 'source':str(source), 'files':{}}
    manifest['git_commit'] = subprocess.check_output(['git','rev-parse','HEAD'],cwd=source,text=True).strip()
    for path in selected:
        relative = path.relative_to(source)
        dest = work/relative
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(path, dest)
        manifest['files'][str(relative)] = hashlib.sha256(path.read_bytes()).hexdigest()
    (output/'source-manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2))
    scripts = Path(__file__).resolve().parent
    (output/'environment.json').write_text(json.dumps({
        'time':manifest['time'], 'os':platform.platform(), 'machine':platform.machine(),
        'java':subprocess.run(['java','-version'],capture_output=True,text=True,check=True).stderr,
        'hardware':subprocess.check_output(['sysctl','-n','machdep.cpu.brand_string'],text=True).strip() if sys.platform=='darwin' else platform.processor(),
        'memory_bytes':str(os.sysconf('SC_PAGE_SIZE')*os.sysconf('SC_PHYS_PAGES')),
        'processors':str(os.cpu_count()),
    },ensure_ascii=False,indent=2))
    reproduction = {'scripts':{p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in scripts.iterdir() if p.suffix in {'.java','.py'}},'python':sys.version}

    def run(command, log, cwd=work, timeout=240):
        with (output/log).open('w') as stream:
            result = subprocess.run(command, cwd=cwd, stdout=stream, stderr=subprocess.STDOUT, timeout=timeout)
        if result.returncode:
            raise RuntimeError(f'Exit {result.returncode}; inspect {output/log}')

    run(['./mvnw', 'clean', 'verify'], 'maven-verify.log')
    run(['./mvnw', '-q', 'dependency:build-classpath', '-Dmdep.outputFile=target/report-classpath.txt'], 'classpath.log')
    run([sys.executable, str(scripts/'make_fixture.py'), str(output/'correlated-fixture.json')], 'fixture.log')
    reproduction['fixture_sha256'] = hashlib.sha256((output/'correlated-fixture.json').read_bytes()).hexdigest()
    (output/'reproduction-manifest.json').write_text(json.dumps(reproduction,indent=2))
    jars = list((work/'target').glob('adjust-java-lib-*.jar'))
    if len(jars) != 1:
        raise RuntimeError(f'Expected exactly one built SDK JAR, got {jars}')
    jar = jars[0]
    classpath = (work/'target/report-classpath.txt').read_text().strip()+os.pathsep+str(jar)
    classes = output/'probe-classes'
    classes.mkdir()
    run(['javac', '--release', '20', '-proc:none', '-cp', classpath, '-d', str(classes), str(scripts/'AdjustReportProbe.java')], 'probe-compile.log')
    run(['java', '-Xmx1g', '-Djna.encoding=UTF-8', '-cp', str(classes)+os.pathsep+classpath, 'AdjustReportProbe', str(output/'probe-results.json'), str(output/'correlated-fixture.json')], 'probe.log', timeout=180)
    report = next((work/'target/surefire-reports').glob('TEST-*.xml'))
    suite = ET.parse(report).getroot()
    (output/'junit-summary.json').write_text(json.dumps({'tests':suite.attrib, 'testcases':[t.attrib for t in suite.findall('testcase')]}, indent=2))
    with ZipFile(jar) as archive:
        build = {'name':jar.name, 'bytes':jar.stat().st_size, 'sha256':hashlib.sha256(jar.read_bytes()).hexdigest(), 'normal_classpath_layout':'com/navfirst/adjust/lib/services/AdjustLibService.class' in archive.namelist(), 'boot_inf_present':any(n.startswith('BOOT-INF/') for n in archive.namelist()), 'native_resources':[n for n in archive.namelist() if n.endswith(('.so','.dylib'))]}
    (output/'build.json').write_text(json.dumps(build, indent=2))
    results = json.loads((output/'probe-results.json').read_text())
    print(f"Evidence: {output}; checks passed={results['passed']}, failed={results['failed']}")


if __name__ == '__main__':
    main()
