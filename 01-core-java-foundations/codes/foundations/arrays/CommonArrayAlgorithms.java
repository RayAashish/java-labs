package foundations.arrays;

import java.util.Arrays;

public class CommonArrayAlgorithms {

    static int[] maxMin(int[] nums){
        int max = Integer.MIN_VALUE;
        int min = Integer.MAX_VALUE;
        for (int n : nums){
            max = Math.max(max, n);
            min = Math.min(min, n);
        }
        return new int[]{max, min};
    }
    static void reverseArr(int[] arr){
        int l = 0, r = arr.length - 1;
        while (l < r){
            arr[l] = arr[l] + arr[r];
            arr[r] = arr[l] - arr[r];
            arr[l] = arr[l] - arr[r];
            l++;
            r--;
        }
    }

    static void rotateRightByK(int[] nums, int k){
        k = k % nums.length;
        rotateHelper(nums, 0, nums.length - 1);
        rotateHelper(nums, 0, k - 1);
        rotateHelper(nums, k, nums.length - 1);
    }
    static void rotateHelper(int[] nums, int s, int e){
        while (s < e){
            nums[s] = nums[s] + nums[e];
            nums[e] = nums[s] - nums[e];
            nums[s] = nums[s] - nums[e];
            s++;
            e--;
        }
    }

    static void prefixSum(int[] nums){
        for (int i = 1; i < nums.length; i++){
            nums[i] += nums[i - 1];
        } 
    }

    /**
     * @param nums
     * @return max
     * It works on a simple idea, if the ongoing sum
     * comes below 0, it will decrease the sum in future not increase
     * so it's better to start from there instead of carrying it for future
     * When we take maxSum = 0 & currentSum = 0, it works for positive, neg array only
     * but if the array contains all negative element, we need to assign maxSum = nums[0]
     */
    static int maxSubArraySum(int[] nums){ //Using kadane's algorithm
        int maxSum = nums[0], currentSum = 0;
        for (int n : nums){
            currentSum += n;
            maxSum = Math.max(maxSum, currentSum);
            if (currentSum <= 0)
                currentSum = 0;
        }
        return maxSum;
    }   

    static int binarySearch(int[] nums, int l, int h, int key){
        if (l <= h){
            int mid = (l + h) / 2;
            if (nums[mid] == key)
                return mid;
            else if(nums[mid] > key)
                return binarySearch(nums, l, mid - 1, key);
            else
                return binarySearch(nums, mid + 1, h, key);
        }
        return -1;
    }

    static int maxSumOfK(int[] nums, int k){
        int ans = 0;
        for (int i = 0; i < k; i++){
            ans += nums[i];
        }
        int windowSum = ans;
        int j = 0;
        for (int i = k; i < nums.length; i++){
            windowSum -= nums[j++];
            windowSum += nums[i];
            ans = Math.max(ans, windowSum);
        }
        return ans;
    }
    static void frequencyArrayOfString(String str){
        int[] chars = new int[26];
        for (char ch : str.toCharArray()){
            chars[ch - 'a']++;
        }
        System.out.println(Arrays.toString(chars));
    }
    public static void main(String[] args) {
        int[] nums = {-98, 7, 92, -7, 6, 88, 120};
        int[] nums2 = {1, 2, 3, 4, 5, 6, 7};
        int[] prefix = {1, 3, 5, 7, 9};
        int[] neg = {-9, -8, -2, -1, -9, -8, -8, -99};
        int[] sorted = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14};
        // System.out.println(Arrays.toString(maxMin(nums)));
        // reverseArr(nums);
        // System.out.println(Arrays.toString(nums));
        // rotateRightByK(nums2, 3);
        // System.out.println(Arrays.toString(nums2));
        // prefixSum(prefix);
        // System.out.println(Arrays.toString(prefix));
        // System.out.println(maxSubArraySum(neg));
        // System.out.println(binarySearch(sorted, 0, sorted.length - 1, 11));
        // System.out.println(maxSumOfK(nums, 3));
        frequencyArrayOfString("null");

    }
}
