package com.xishanju.testSQL.fildname;

class TTTT {
    public double findMedianSortedArrays(int[] nums1, int[] nums2) {
        if (nums1 == null) {
            return (nums2[0] + nums2[nums2.length - 1]) / 2;
        }
        if (nums2 == null) {
            return (nums1[0] + nums1[nums1.length - 1]) / 2;
        }

        int m = nums1.length;
        int n = nums2.length;
        double bijiao = bijiao(0, m - 1, nums1, 0, n - 1, nums2);

        return bijiao;
    }

    public double bijiao(int begin1, int end1, int[] s1, int begin2, int end2, int[] s2) {
        if (begin1 > end1 || begin2 > end2) {
            return 0.0;
        }
        int z1 = (s1[begin1] + s1[end1]) / 2;
        int z2 = (s2[begin2] + s2[end2]) / 2;
        if (s1.length == 1 || s2.length == 1) {
            return (z1 + z2) / 2;
        }
        int m = s1.length;
        int n = s2.length;


        if (z1 > z2) {
            return bijiao(0, (int) Math.floor(z1), s1, z2 + 1, n - 1, s2);
        } else {
            return bijiao(z1 + 1, m - 1, s1, 0, z2 - 1, s2);
        }


    }

    public static void main(String[] args) {
        int[] num1 = {1, 2};
        int[] num2 = {3, 4};
        double medianSortedArrays = new TTTT().findMedianSortedArrays(num1, num2);
        System.out.println(medianSortedArrays);
    }
}