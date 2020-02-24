package de.fs.fintech.geogame;

import android.content.Intent;
import android.graphics.Point;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class PortalGameSnippetActivity extends AppCompatActivity {

    private static final Logger logger = LoggerFactory.getLogger(PortalGameSnippetActivity.class);
    private boolean is_already_one_selected = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_portal_game_snippet);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        logger.info("Width: " + width + ", Height: " + height);

        ArrayAdapter<String> arrayAdapter;

        final TextView txt_timer = (TextView) findViewById(R.id.txt_timer);
        final ImageView img_snippet = (ImageView) findViewById(R.id.img_snippet);
        final Spinner spin_first_space = (Spinner) findViewById(R.id.spin_first_space);
        final Spinner spin_second_space = (Spinner) findViewById(R.id.spin_second_space);

        //Replace this with reading from file
        String fileString = "snippet_void_main, snippet_case_default, snippet_jbutton_actionlistener, snippet_for_plusplus; " +
                "1, 13, 15, 19; 3, 14, 16, 20; " +
                "--, void, String, String[], string[], string, double, int, Double, Integer, static, Bundle, new, case, default, JButton, ActionListener, ComponentListener, Action, for, ++";

        String[] rows = fileString.split("; ");                                                     //Rows: File Names, solution id´s & solutions
        String[] file_names = rows[0].split(", ");                                                   //Array with File Names
        final int[] id_solution_one_for_file = make_int_array_from_string(rows[1]);                       //Array with Solution Id´s
        final int[] id_solution_two_for_file = make_int_array_from_string(rows[2]);                       //Array with Solution Id´s for 2nd space
        final String[] solutions = rows[3].split(", ");                                                   //Array with all possible solutions
        final int id_file = (int) Math.floor(Math.random() * file_names.length);                          //Randomly generates the File that is gonna be used
        String file_name = file_names[id_file];
        img_snippet.setImageResource(getResources().getIdentifier(file_name, "drawable", getPackageName()));

        String[] spinner_array = make_solutions_array(id_file, id_solution_one_for_file[id_file], solutions);

        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, spinner_array);
        spin_first_space.setAdapter(arrayAdapter);

        spin_first_space.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(!spin_first_space.getSelectedItem().toString().equals("--")) {
                    String selected = parent.getSelectedItem().toString();
                    spin_first_space.setEnabled(false);
                    spin_first_space.setClickable(false);
                    if(is_already_one_selected) on_both_selected(new String[]{solutions[id_solution_one_for_file[id_file]], solutions[id_solution_two_for_file[id_file]]});
                    else is_already_one_selected = true;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        spinner_array = make_solutions_array(id_file, id_solution_two_for_file[id_file], solutions);
        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, spinner_array);
        spin_second_space.setAdapter(arrayAdapter);

        spin_second_space.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(!spin_second_space.getSelectedItem().toString().equals("--")) {
                    String selected = parent.getSelectedItem().toString();
                    spin_second_space.setEnabled(false);
                    spin_second_space.setClickable(false);
                    if(is_already_one_selected) on_both_selected(new String[]{solutions[id_solution_one_for_file[id_file]], solutions[id_solution_two_for_file[id_file]]});
                    else is_already_one_selected = true;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        txt_timer.setText("30.00");

        new CountDownTimer(60000, 10) {
            @Override
            public void onTick(long millisUntilFinished) {
                double d_millisUntilFinished = millisUntilFinished;
                txt_timer.setText(String.format("%.2f", (d_millisUntilFinished/1000)));
            }

            @Override
            public void onFinish() {
                on_both_selected(false, new String[]{solutions[id_solution_one_for_file[id_file]], solutions[id_solution_two_for_file[id_file]]});
            }
        }.start();


    }

    private String[] make_solutions_array(int id_file, int id_solution, String[] solutions) {
        String[] values = new String[6];
        int solution_space = (int) Math.floor(Math.random() * 5 + 1);
        values[0] = solutions[0];
        for(int i = 1; i < values.length; i++) {

            if(i == solution_space) {                                           //set actual solution to random field
                logger.info("Inserting solution - " + solutions[id_solution] + " - in Array at index - " + i);
                values[i] = solutions[id_solution];
                continue;
            }
            int generate = (int) Math.floor(Math.random() * (solutions.length - 1) + 1);

            if(generate == id_solution                                        //Random = Solution
                    || ArrayUtils.contains(values, solutions[generate])) {   //Random is already in list
                //logger.info("Redo! Generate = " + generate + " - Solutions[Generate] = " + solutions[generate]);
                i--;
                continue;
            } else {
                logger.info("Found fitting Solution - " + solutions[generate] + " - for spot - " + i + " - with index - " + generate);
                values[i] = solutions[generate];
            }

        }

        for(int i = 0; i < values.length; i++) logger.info(values[i]);
        return values;
    }

    private int[] make_int_array_from_string(String s) {
        String[] string_array_helper = s.split(", ");
        int[] ia = new int[string_array_helper.length];
        for(int i = 0; i < ia.length; i++) ia[i] = Integer.parseInt(string_array_helper[i]);
        return ia;
    }

    public void on_both_selected(String[] solutions) {
        on_both_selected(true, solutions);
    }

    public void on_both_selected(boolean intime, String[] solutions) {
        int i_correct_answers = 0;
        String s_selected_result1 = ((Spinner) findViewById(R.id.spin_first_space)).getSelectedItem().toString();
        String s_selected_result2 = ((Spinner) findViewById(R.id.spin_second_space)).getSelectedItem().toString();

        //Get real results from DB

        String s_actual_result1 = solutions[0];
        String s_actual_result2 = solutions[1];


        if(s_selected_result1.equals(s_actual_result1)) i_correct_answers++;
        if(s_selected_result2.equals(s_actual_result2)) i_correct_answers++;

        Intent i_return_intent = new Intent();
        i_return_intent.putExtra("KEY_CORRECT_ANSWERS", i_correct_answers);

        if(intime) setResult(RESULT_OK, i_return_intent);
        else setResult(RESULT_CANCELED, i_return_intent);
        finish();
    }


}
