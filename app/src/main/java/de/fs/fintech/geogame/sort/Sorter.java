package de.fs.fintech.geogame.sort;

import java.util.ArrayList;

import de.fs.fintech.geogame.data.LeaderBoardAlpha;
import de.fs.fintech.geogame.parcelable.LeaderBoardAlphaParcel;

/**
 * Created by nicolaskepper on 21.06.17.
 */

public class Sorter {

    public static void quicksort(ArrayList<LeaderBoardAlphaParcel> array)
    {
        quicksort(array, 0, array.size() - 1);
    }

    private static void quicksort(ArrayList<LeaderBoardAlphaParcel> array, int left, int right)
    {
        if(right == left) return;
        if(right - left == 1)
        {
            if(array.get(right).score < array.get(left).score)
            {
                swap(right, left, array);
            }
            return;
        }

        int pivot = left, p_l = left + 1, p_r = right;

        while(p_l < p_r)
        {
            while(p_l < right && array.get(p_l).score < array.get(pivot).score)
                p_l++;
            while(p_r >= pivot && array.get(p_r).score > array.get(pivot).score)
                p_r--;
            if(!(p_l > p_r))
                swap(p_l, p_r, array);
        }

        swap(p_r, pivot, array);

        quicksort(array, left, p_r);
        quicksort(array, p_l, right);
    }
    private static void swap(int swap_this_index, int with_this_index, ArrayList<LeaderBoardAlphaParcel> array)
    {
        if(swap_this_index == with_this_index) return;
        LeaderBoardAlphaParcel help = array.get(swap_this_index);
        array.set(swap_this_index, array.get(with_this_index));
        array.set(with_this_index, help);
    }

    public static void invert(ArrayList<LeaderBoardAlphaParcel> array) {
        for(int i = 0; i < array.size() / 2; i++) {
            swap(i, array.size() - (1 + i), array);
        }
    }
}
