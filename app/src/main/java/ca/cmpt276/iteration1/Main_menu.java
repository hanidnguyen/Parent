package ca.cmpt276.iteration1;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;

import java.util.ArrayList;

import ca.cmpt276.iteration1.model.Child;
import ca.cmpt276.iteration1.model.Children_Manager;
import ca.cmpt276.iteration1.model.Coin_Flip;
import ca.cmpt276.iteration1.model.Coin_Flip_Manager;
import ca.cmpt276.iteration1.model.Edited_Child;
import ca.cmpt276.iteration1.model.Task;
import ca.cmpt276.iteration1.model.Task_Manager;

/*
       Main Menu class using layout: activity_main_menu
       5 transparent buttons takes user to 5 different features of the app namely:
       -    Children configuration
       -    Coin flip history
       -    Coin flip
       -    Timer
       -    About Us

 */

public class Main_menu extends AppCompatActivity {
    private Children_Manager children_manager;
    private Coin_Flip_Manager coin_manager;
    private Task_Manager task_manager;
    private ActivityResultLauncher<Intent> coin_flip_launcher;
    private ActivityResultLauncher<Intent> config_launcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        children_manager = Children_Manager.getInstance();
        coin_manager = Coin_Flip_Manager.getInstance();
        task_manager = Task_Manager.getInstance();

        setup_TEST_task();

        setup_coin_flip_launcher();
        setup_config_launcher();
        setup_config_button();
        setup_coin_flip();
        setup_tasks();
        setup_history_button();
        setup_timer_button();

    }

    private void setup_TEST_task() {
        Task task1 = new Task(children_manager.getChildren_list(),"drink milk");
        Task task2 = new Task(children_manager.getChildren_list(),"play games");
        Task task3 = new Task(children_manager.getChildren_list(),"choose fruit");
        Task task4 = new Task(children_manager.getChildren_list(),"watch tv");
        Task task5 = new Task(children_manager.getChildren_list(),"choose gummy");
        task_manager.add_task(task1);
        task_manager.add_task(task2);
        task_manager.add_task(task3);
        task_manager.add_task(task4);
        task_manager.add_task(task5);
    }

    private void setup_config_launcher() {
        config_launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if(result.getResultCode() == Activity.RESULT_OK){
                        Intent data = result.getData();
                        //if there's configured children
                        if(data != null){
                            ArrayList<String> children_list = data.getStringArrayListExtra("CHILDREN_LIST");
                            ArrayList<Edited_Child> edited_children_list = data.getParcelableArrayListExtra("EDITED_CHILDREN");
                            children_manager.clear();
                            for (String name : children_list) {
                                Child new_child = new Child(name);
                                children_manager.addChild(new_child);
                            }
                            if(!edited_children_list.isEmpty()){
                                coin_manager.update_child_name_after_edit(edited_children_list);
                            }

                        } else {
                            children_manager.clear();
                        }
                    }
                }
        );
    }

    private void setup_coin_flip_launcher() {
        coin_flip_launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // There are no request codes
                        Intent data = result.getData();
                        assert data != null;

                        String picker = data.getStringExtra("PICKER");
                        boolean returned_result = data.getBooleanExtra("RESULT",false);
                        String time = data.getStringExtra("TIME");

                        if(!picker.equals("No name")){
                            int index = children_manager.getChildren_list().indexOf(picker);
                            children_manager.update_queue(index); //child who last picked be moved to end of queue.
                        }

                        Coin_Flip new_flip = new Coin_Flip(picker,returned_result,time);
                        coin_manager.add_flip(new_flip);
                    }
                });
    }

    private void setup_config_button() {
        Button configure_children = findViewById(R.id.config_button);
        configure_children.setOnClickListener(view -> {
            Intent intent = new Intent(Main_menu.this, Children_Config.class);
            intent.putExtra("CHILDREN_LIST", children_manager.getChildren_list());
            config_launcher.launch(intent);
        });
    }

    private void setup_coin_flip() {
        Button button = findViewById(R.id.coin_flip_button);
        button.setOnClickListener(view -> {
            Intent intent = new Intent(Main_menu.this, Coin_Flip_Config.class);
            intent.putExtra("CHILDREN_LIST",children_manager.getChildren_list());
            coin_flip_launcher.launch(intent);
        });
    }

    private void setup_history_button() {
        Button button = findViewById(R.id.coin_history_button);
        button.setOnClickListener(view -> {
            Intent intent = new Intent(Main_menu.this, Coin_flip_history.class);
            intent.putExtra("COIN_FLIP_LIST", coin_manager.getCoin_flip_list());
            startActivity(intent);
        });
    }

    private void setup_tasks() {
        Button button = findViewById(R.id.task_button);
        button.setOnClickListener(view->{
            Intent intent = new Intent(Main_menu.this, Task_List_Activity.class);
            intent.putParcelableArrayListExtra("TASK_LIST", task_manager.getTask_list());
            startActivity(intent);
        });
    }

    private void setup_timer_button() {
        Button button = findViewById(R.id.timer_button);
        button.setOnClickListener(view -> {
            Intent intent = new Intent(Main_menu.this, Timeout_Timer.class);
            startActivity(intent);
        });
    }



    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        finish();
        return super.onOptionsItemSelected(item);
    }
}