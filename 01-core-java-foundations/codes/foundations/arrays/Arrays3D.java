package foundations.arrays;

import java.util.Arrays;

public class Arrays3D {

    private static void arrays3D(){
        int[][][] cube = new int[3][2][4];
        for (int i = 0; i < 3; i++){
            for (int j = 0; j < 2; j++){
                for (int k = 0; k < 4; k++){
                    int x = (int)(Math.random() * 10);
                    cube[i][j][k] = x;
                }
            }
        }
        System.out.println(Arrays.deepToString(cube));
    }
    public static void main(String[] args) {
        arrays3D();
    }
}
