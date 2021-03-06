package com.example.schoolapp;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.schoolapp.Extra.GlobalVar;
import com.example.schoolapp.Extra.ModuleData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class GpaCalcActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    public TextView result;         //Located in activity_gpa.xml, as a giant text view used to show result

    public Spinner module;         //Located in activity_gpa.xml, shown at the top, representing the current module

    public Button buttonCalc;       //Located at the bottom of activity_gpa.xml: reset all EditText and Module to Module 1
    public Button buttonSave;       //Located before the result box in activity_gpa.xml: total the current value and increase the module counter (Module 1 -> Module 2)

    double totalMark = 0;             //Variable to store the total value
    double totalCredit = 0;           //Variable to store the Total sum of credits

    public EditText examGrade;      //Input Type: decimals for examMarks
    public EditText cwGrade;        //Input Type: decimals for courseworkMarks
    public EditText examPercentage; //Input Type: decimals for exam percentage (weight)
    public EditText creditModule;   //Input Type: decimals for credit module (in multiples of 10)

    String moduleId;                //Module ID: Currently selected module (eg. PADS, OSD, or OSSN)

    DatabaseReference databaseModuleData;

    FirebaseAuth auth;
    FirebaseUser firebaseUser;

    private static final String LOG_TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gpa_calc);

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        databaseModuleData = FirebaseDatabase.getInstance().getReference("ModuleData");

        result = (TextView) findViewById(R.id.result);          //result: id for TextView with big box
        module = (Spinner) findViewById(R.id.spinnerModule1);   //Drop down menu for modules (eg. PADS, OSD, OSSN)

        //For spinner(drop down menu) customization
        //Get the array
        //spinner_item -> file that contains all the text attribute of the spinner item
        ArrayAdapter<CharSequence> moduleAdapter = ArrayAdapter.createFromResource(this, getModuleArray(), R.layout.spinner_item);
        moduleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        module.setAdapter(moduleAdapter);
        module.setOnItemSelectedListener(this);

        buttonSave = (Button) findViewById(R.id.buttonSave);    //initialize "save" button
        buttonCalc = (Button) findViewById(R.id.buttonCalc);    //initialize "calculate" button

        //"save" button function
        //Check if it contains any null value in any of the EditText (input box)
        //If there is any empty field, prompt alert dialog
        //If there is no empty field, perform mark calculation, show user that his marks are saved, and clear all fields
        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Initialize all the fields to string for validation purpose (validate whether there is any empty field)
                examGrade = (EditText) findViewById(R.id.editTextGrade1);
                String sExamGrade = examGrade.getText().toString();
                cwGrade = (EditText) findViewById(R.id.editTextCourseWorkGrade1);
                String sCwGrade = cwGrade.getText().toString();
                examPercentage = (EditText) findViewById(R.id.editTextPercentage1);
                String sExamPercentage = examPercentage.getText().toString();
                creditModule = (EditText) findViewById(R.id.editTextModuleCredit1);
                String sCreditModule = creditModule.getText().toString();

                double dExamGrade;
                double dCwGrade;
                double dExamPercentage;
                double dCreditModule;

                result.setText(String.valueOf((sExamGrade.matches("") || sCwGrade.matches("") || sExamPercentage.matches("") || sCreditModule.matches(""))));
                if (sExamGrade.matches("") || sCwGrade.matches("") || sExamPercentage.matches("") || sCreditModule.matches("")) {
                    checkNull(true);
                } else {
                    dExamGrade = Double.parseDouble(sExamGrade);
                    dCwGrade = Double.parseDouble(sCwGrade);
                    dExamPercentage = Double.parseDouble(sExamPercentage);
                    dCreditModule = Double.parseDouble(sCreditModule);

                    if (dCreditModule % 10 != 0 || !(dCreditModule <= 30 && dCreditModule >= 10) || !(dCwGrade <= 100 && dCwGrade >= 0) || !(dExamGrade <= 100 && dExamGrade >= 0) || !(dExamPercentage <= 100 && dExamPercentage >= 0)) {
                        creditAlert();
                    } else {
                        addModuleData();
                        double tempMark = calcMark(dExamGrade, dCwGrade, dExamPercentage, dCreditModule); //mark calculation
                        totalMark += tempMark;                //Sum temporary mark into total mark
                        totalCredit += (dCreditModule / 10.0);  //Sum the credit module into the total module

                        result.setText("Saved:" + moduleId); //Show user that their progress is saved
                    }
                }
            }
        });

        //"Calculate" button function
        //If there is no total or credit value then prompt a toast message
        //If there is no empty field, perform mark calculation, show user that his final result
        buttonCalc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (totalMark == 0 || totalCredit == 0) {
                    Toast.makeText(GpaCalcActivity.this, "There are no mark to be calculated", Toast.LENGTH_LONG).show();
                } else {
                    double finalmark = totalMark / totalCredit;
                    String finalMark = String.valueOf(finalmark);
                    result.setText(getResult(finalmark) + "\nFinal Mark: " + finalMark);
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        databaseModuleData.child(firebaseUser.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                totalMark = 0;
                totalCredit = 0;
                for (DataSnapshot moduleDataSnapshot : dataSnapshot.getChildren()) {

                    ModuleData moduleData = moduleDataSnapshot.getValue(ModuleData.class);
                    double tempExamGrade = moduleData.getExamGrade();
                    double tempCwGrade = moduleData.getCwGrade();
                    double tempExamPercentage = moduleData.getExamPercentage();
                    double tempCreditModule = moduleData.getCreditModule();

                    double tempMark = calcMark(tempExamGrade, tempCwGrade, tempExamPercentage, tempCreditModule);
                    totalMark += tempMark;
                    totalCredit += (tempCreditModule / 10.0);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    //Return grade based on finalResult
    public String getResult(double finalResult) {
        String sResult = "";
        if (finalResult >= 70) {
            sResult = "GPA - 4.0 (A)\n FIRST-CLASS HONOURS";
        } else if (finalResult >= 65 && finalResult <= 69) {
            sResult = "GPA - 3.7 (B)\n UPPER SECOND-CLASS HONOURS";
        } else if (finalResult >= 60 && finalResult <= 64) {
            sResult = "GPA - 3.3 (B)\n UPPER SECOND-CLASS HONOURS";
        } else if (finalResult >= 55 && finalResult <= 59) {
            sResult = "GPA - 3 (C)\n LOWER SECOND-CLASS HONOURS";
        } else if (finalResult >= 50 && finalResult <= 59) {
            sResult = "GPA - 2.7 (C)\n LOWER SECOND-CLASS HONOURS";
        } else if (finalResult >= 45 && finalResult <= 49) {
            sResult = "GPA - 2.3 (D)\n ORDINARY/UNCLASSIFIED";
        } else {
            sResult = "FAIL\nREMODULE";
        }
        return sResult;
    }

    //Null checker
    //If true prompt the alert dialog
    //If false do nothing
    public void checkNull(boolean nullInput) {
        if (nullInput == true) {
            AlertDialog.Builder x = new AlertDialog.Builder(GpaCalcActivity.this);

            x.setTitle("MISSING VALUE");
            x.setMessage("No EMPTY box is allowed\nPlease check the inputs ^-^");
            x.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            AlertDialog alert = x.create();
            alert.show();
        }
    }

    //Mark calculation algorithm
    public double calcMark(double examGrade, double cwGrade, double examPercentage, double creditModule) {
        double calculatedMark = ((examGrade * examPercentage / 100.0) + (cwGrade * (100 - examPercentage) / 100.0)) * creditModule / 10.0;
        return calculatedMark;
    }

    //Clear(Reset) every field
    public void clearEditText() {
        examGrade.getText().clear();
        cwGrade.getText().clear();
        examPercentage.getText().clear();
        creditModule.getText().clear();
    }

    //Alert box for wrong entry input
    public void creditAlert() {
        AlertDialog.Builder y = new AlertDialog.Builder(GpaCalcActivity.this);

        y.setTitle("INVALID VALUE");
        y.setMessage("You have entered invalid value\n1.Credit value:10,20, or 30 and divideable by 10\n2.Percentage, Exam grade, and coursework grade must be between 0-100");
        y.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog alert = y.create();
        alert.show();
    }
    //Retrieve the modules (PADS, OSD, OSSN) for the course enrolled
    public static int getModuleArray() {
        switch (GlobalVar.getData()) {
            case ("Bachelor of Arts with Honours in Accounting and Finance"): {
                return R.array.AFmodules;
            }
            case ("Bachelor of Arts with Honours in Business and Marketing"): {
                return R.array.BZNmodules;
            }
            case ("Bachelor of Engineering with Honours in Mechanical Engineering"): {
                return R.array.MECHENGmodules;
            }
            case ("Bachelor of Science with Honours in Computer Science"): {
                return R.array.CSmodules;
            }
            default:
                return -1;
        }
}
    //Function to add the all the entered data in EditText box to the object class ModuleData
    private void addModuleData(){
        double dExamGrade = Double.parseDouble(examGrade.getText().toString());
        double dCwGrade = Double.parseDouble(cwGrade.getText().toString());
        double dExamPercentage = Double.parseDouble(examPercentage.getText().toString());
        double dCreditModule = Double.parseDouble(creditModule.getText().toString());

        double tempMark = calcMark(dExamGrade,dCwGrade,dExamPercentage,dCreditModule);
        double tempCredit = dCreditModule/10.0;
        tempMark/=tempCredit;


        ModuleData moduleData = new ModuleData(dExamGrade,dCwGrade,dExamPercentage,dCreditModule,moduleId,tempMark);


        String userPath = firebaseUser.getUid() + "/" + moduleId;
        databaseModuleData.child(userPath).setValue(moduleData);
    }
    //Function to show in the EditText value that user had entered
    //If the user had not entered any value, leave the EditText box blank
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        moduleId = parent.getItemAtPosition(position).toString();
        examGrade = (EditText) findViewById(R.id.editTextGrade1);
        cwGrade = (EditText) findViewById(R.id.editTextCourseWorkGrade1);
        examPercentage = (EditText) findViewById(R.id.editTextPercentage1);
        creditModule = (EditText) findViewById(R.id.editTextModuleCredit1);
        clearEditText();   //Clear the EditText box everytime the item change
        try {
            databaseModuleData.child(firebaseUser.getUid()).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        //If the moduleId (the selected module) is the same as current moduleDataSnapshot (Eg. PADS == PADS)
                        //Retrieve the value of current data snapshot and show it in the EditText box
                        for (DataSnapshot moduleDataSnapshot : dataSnapshot.getChildren()) {
                                ModuleData moduleData = moduleDataSnapshot.getValue(ModuleData.class);
                                String moduleName = moduleData.getModuleName();
                                if (moduleName.equals(moduleId)&&moduleName!=null) {
                                    double tempExamGrade = moduleData.getExamGrade();
                                    double tempCwGrade = moduleData.getCwGrade();
                                    double tempExamPercentage = moduleData.getExamPercentage();
                                    double tempCreditModule = moduleData.getCreditModule();

                                    examGrade = (EditText) findViewById(R.id.editTextGrade1);
                                    cwGrade = (EditText) findViewById(R.id.editTextCourseWorkGrade1);
                                    examPercentage = (EditText) findViewById(R.id.editTextPercentage1);
                                    creditModule = (EditText) findViewById(R.id.editTextModuleCredit1);

                                    creditModule.setText(String.valueOf(tempCreditModule));
                                    examGrade.setText(String.valueOf(tempExamGrade));
                                    cwGrade.setText(String.valueOf(tempCwGrade));
                                    examPercentage.setText(String.valueOf(tempExamPercentage));
                                }

                        }
                    }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            });
        }
        catch (NullPointerException e) {
            Toast.makeText(GpaCalcActivity.this, "The database is empty", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}


