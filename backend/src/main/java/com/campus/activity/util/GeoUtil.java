package com.campus.activity.util;

/**
 * 地理工具：解析 "lng,lat" 坐标串并计算球面距离（米），用于准确签到的地理围栏。
 */
public class GeoUtil {

    private static final double EARTH_RADIUS = 6371000.0;

    /**
     * 解析 "lng,lat"。
     * 拒绝缺失、多余分量、NaN / Infinity 以及超出经纬度范围的坐标，避免非法值绕过围栏判断。
     */
    public static double[] parse(String lngLat) {
        if (lngLat == null || !lngLat.contains(",")) {
            return null;
        }
        String[] arr = lngLat.split(",");
        if (arr.length != 2) {
            return null;
        }
        try {
            double lng = Double.parseDouble(arr[0].trim());
            double lat = Double.parseDouble(arr[1].trim());
            if (!Double.isFinite(lng) || !Double.isFinite(lat)) {
                return null;
            }
            if (lng < -180 || lng > 180 || lat < -90 || lat > 90) {
                return null;
            }
            return new double[]{lng, lat};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Haversine 距离，单位米 */
    public static double distance(String a, String b) {
        double[] p1 = parse(a);
        double[] p2 = parse(b);
        if (p1 == null || p2 == null) {
            return Double.MAX_VALUE;
        }
        double dLat = Math.toRadians(p2[1] - p1[1]);
        double dLng = Math.toRadians(p2[0] - p1[0]);
        double lat1 = Math.toRadians(p1[1]);
        double lat2 = Math.toRadians(p2[1]);
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * EARTH_RADIUS * Math.asin(Math.sqrt(h));
    }
}
