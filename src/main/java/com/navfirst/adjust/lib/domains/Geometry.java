package com.navfirst.adjust.lib.domains;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** 站网共用的坐标、向量和协方差矩阵，长度单位为米，角度单位为度。 */
public final class Geometry {
    private Geometry() {
    }

    /**
     * 东、北、天三个方向的分量；坐标和标准差单位为米，标准化残差无量纲。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ENU {

        /** 东向分量；坐标、向量和标准差的单位为米，标准化残差为无量纲。 */
        @SerializedName("east")
        private double east;

        /** 北向分量；坐标、向量和标准差的单位为米，标准化残差为无量纲。 */
        @SerializedName("north")
        private double north;

        /** 天向分量，正方向朝上；坐标、向量和标准差的单位为米，标准化残差为无量纲。 */
        @SerializedName("up")
        private double up;
    }

    /**
     * WGS84 大地坐标，经纬度单位为度，椭球高单位为米。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WGS84Coordinate {

        /** WGS84 大地纬度，单位度，范围 [-90, 90]，北纬为正。 */
        @SerializedName("latitude_deg")
        private double latitudeDeg;

        /** WGS84 大地经度，单位度，范围 [-180, 180]，东经为正。 */
        @SerializedName("longitude_deg")
        private double longitudeDeg;

        /** 相对 WGS84 椭球面的椭球高，单位米。 */
        @SerializedName("ellipsoid_height_m")
        private double ellipsoidHeight;
    }

    /**
     * WGS84 地心地固直角坐标，单位为米。X 轴指向纬度 0°、经度 0°，
     * Y 轴指向纬度 0°、东经 90°，Z 轴指向北极。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ECEFCoordinate {

        /** ECEF X 轴分量，单位米；用于 stdDevXYZ 时表示 X 方向标准差。 */
        @SerializedName("x_m")
        private double x;

        /** ECEF Y 轴分量，单位米；用于 stdDevXYZ 时表示 Y 方向标准差。 */
        @SerializedName("y_m")
        private double y;

        /** ECEF Z 轴分量，单位米；用于 stdDevXYZ 时表示 Z 方向标准差。 */
        @SerializedName("z_m")
        private double z;
    }

    /** 行优先的 3×3 协方差矩阵，单位为平方米。默认零矩阵不满足 required 策略。 */
    @EqualsAndHashCode
    @ToString
    public static class Matrix3 {
        /** 按行优先存放的 9 个矩阵元素，索引为 row * 3 + column；协方差单位平方米，按东、北、天三个方向排列。 */
        @SerializedName("data")
        private double[] data = new double[9];

        public Matrix3() {
        }

        public Matrix3(double[] data) {
            setData(data);
        }

        public double[] getData() {
            return data.clone();
        }

        public void setData(double[] data) {
            if (data == null || data.length != 9) {
                throw new IllegalArgumentException("Matrix3 requires exactly 9 row-major values");
            }
            this.data = data.clone();
        }

        public double at(int row, int column) {
            if (row < 0 || row >= 3 || column < 0 || column >= 3) {
                throw new IndexOutOfBoundsException("Matrix3 index outside [0, 3)");
            }
            return data[row * 3 + column];
        }

        public static Matrix3 diagonal(double eastVariance, double northVariance, double upVariance) {
            return new Matrix3(new double[]{eastVariance, 0, 0, 0, northVariance, 0, 0, 0, upVariance});
        }

        public static Matrix3 fromStdDev(double east, double north, double up) {
            if (!Double.isFinite(east) || !Double.isFinite(north) || !Double.isFinite(up)
                    || east < 0 || north < 0 || up < 0) {
                throw new IllegalArgumentException("Standard deviations must be finite and non-negative");
            }
            return diagonal(east * east, north * north, up * up);
        }
    }
}
