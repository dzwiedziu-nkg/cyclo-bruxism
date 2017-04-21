package pl.nkg.brq.android.ui;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import pl.nkg.brq.android.R;
import pl.nkg.brq.android.network.NetworkAccessLogin;
import pl.nkg.brq.android.network.NetworkAccessRegister;

/**
 * Created by aaa on 2016-11-06.
 */

public class LoginActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 801;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 802;

    EditText userNameEditText;
    EditText passwordEditText;

    EditText userNameRegisterEditText;
    EditText passwordRegisterEditText;
    EditText passwordRepeatRegisterEditText;
    Button registerButton;
    TextView warningTextView;

    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        if ( !sharedPreferences.getString(getString(R.string.pref_user_logged_key), "").equals("") ) {
            startMainActivity();
        }

        userNameEditText = (EditText)findViewById(R.id.username_edit_text);
        passwordEditText = (EditText)findViewById(R.id.password_edit_text);
    }

    /*
    Metoda wywoływana po kliknięciu w przycisk "sign in".
    Sprawdza poprawność nazwy użytkownika i hasła i przechodzi do właściwej aplikacji.
     */
    public void onLogin(View view) throws IOException, InterruptedException, ExecutionException {
        String userName = userNameEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        if ( userName.equals("") ) {
            Toast.makeText(getApplicationContext(), R.string.enter_user_name_hint_toast,Toast.LENGTH_LONG).show();
            return;
        }

        if ( password.equals("") ) {
            Toast.makeText(getApplicationContext(), R.string.enter_password_hint_toast,Toast.LENGTH_LONG).show();
            return;
        }

        // Sprawdzamy czy jest połaczenie internetowe
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        if (activeNetworkInfo == null || !activeNetworkInfo.isConnected()){
            Toast.makeText(getApplicationContext(), R.string.no_internet_toast, Toast.LENGTH_LONG).show();
            return;
        }

        // Wysyłamy na serwer zapytanie o poprawność loginu i hasła
        // Na ekranie wyświetlane jest odpowiednie powiadomienie w przypadku niepoprawnego hasła
        // lub niemożliwości połączenia z internetem
        try {
            String response =  new NetworkAccessLogin().execute(userName, password).get();
            if ( response.equals("true") ) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(getString(R.string.pref_user_logged_key), userName);
                editor.commit();

                startMainActivity();
            } else if (response.equals("false")){
                Toast.makeText(getApplicationContext(), getString(R.string.wrong_password_toast), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e){
            Toast.makeText(getApplicationContext(), R.string.network_problems_toast, Toast.LENGTH_SHORT).show();
            Log.d("myApp", e.toString());
        }
    }

    /*
    Metoda wywołująca formularz do rejestracji nowego użytkownika. Obsługuje interfejs oraz listenery odpowiednich elementów.
     */
    public void onRegister(View view){
        final Dialog myDialog = new Dialog(this);
        myDialog.setContentView(R.layout.register_form);
        myDialog.setTitle(R.string.registration_dialog_title);

        myDialog.show();

        userNameRegisterEditText = (EditText)myDialog.findViewById(R.id.username_register);
        userNameRegisterEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                checkRegisterButton();
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        passwordRegisterEditText = (EditText)myDialog.findViewById(R.id.password_register);
        passwordRegisterEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                checkRegisterButton();
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        passwordRepeatRegisterEditText = (EditText)myDialog.findViewById(R.id.password_register_repeat);
        passwordRepeatRegisterEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                checkRegisterButton();
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        registerButton = (Button) myDialog.findViewById(R.id.button_register_done);
        warningTextView = (TextView) myDialog.findViewById(R.id.repeat_password_warning);
        registerButton.setEnabled(false);
        registerButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                String newUserName = ((EditText)myDialog.findViewById(R.id.username_register)).getText().toString();
                String newUserPassword = ((EditText)myDialog.findViewById(R.id.password_register)).getText().toString();
                String newUserPasswordRepeat = ((EditText)myDialog.findViewById(R.id.password_register_repeat)).getText().toString();
                Toast registerToast;

                if ( newUserPassword.equals(newUserPasswordRepeat) ) {
                    sendRegisterInfo(newUserName, newUserPassword);
                } else {
                    registerToast = Toast.makeText(getApplicationContext(),
                            getString(R.string.registration_repeat_failure_toast),
                            Toast.LENGTH_SHORT);
                    registerToast.show();
                }

                myDialog.dismiss();
            }
        });
    }

    /*
    Metoda sprawdza czy odpowiednie dane są wprowadzone do formularza rejestracji i czy przecisk "register" powinien być aktywny.
     */
    public void checkRegisterButton(){
        if( userNameRegisterEditText.getText().toString().equals("") ||
            passwordRegisterEditText.getText().toString().equals("") ||
            passwordRepeatRegisterEditText.getText().toString().equals("") ||
            !passwordRegisterEditText.getText().toString().equals(passwordRepeatRegisterEditText.getText().toString()) ) {
            registerButton.setEnabled(false);
        } else {
            registerButton.setEnabled(true);
        }

        if (!passwordRegisterEditText.getText().toString().equals(passwordRepeatRegisterEditText.getText().toString()) &&
            !userNameRegisterEditText.getText().toString().equals("") &&
            !passwordRegisterEditText.getText().toString().equals("") ) {
            warningTextView.setVisibility(View.VISIBLE);
        } else {
            warningTextView.setVisibility(View.GONE);
        }
    }

    /*
    Metoda wysyła dane nowego użytkownika na serwer.
     */
    public void sendRegisterInfo(String userName, String password) {
        String response;
        try {
            response =  new NetworkAccessRegister().execute(userName, password).get();
            if ( response.equals("true") ) {
                Toast.makeText(getApplicationContext(), R.string.registartion_success_toast, Toast.LENGTH_SHORT).show();
            } else if (response.equals("false")){
                Toast.makeText(getApplicationContext(), R.string.registration_failure_already_used_toast, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e){
            Toast.makeText(getApplicationContext(), R.string.network_problems_toast, Toast.LENGTH_SHORT).show();
            Log.d("myApp", e.toString());
        }
    }

    public void startMainActivity(){
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
        finish();
    }
}
