package com.example.criptografiadearquivos;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.criptografiadearquivos.utils.CryptoUtils;

import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_FILE_REQUEST_CODE = 1;
    private Uri selectedFileUri;
    private String selectedFileName;
    private TextView tvStatus;
    private Button btnEncrypt, btnDecrypt;
    private boolean isEncrypting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnSelectFile = findViewById(R.id.btn_select_file);
        btnEncrypt = findViewById(R.id.btn_encrypt);
        btnDecrypt = findViewById(R.id.btn_decrypt);
        tvStatus = findViewById(R.id.tv_status);

        btnEncrypt.setEnabled(false);
        btnDecrypt.setEnabled(false);

        btnSelectFile.setOnClickListener(v -> selectFile());

        btnEncrypt.setOnClickListener(v -> {
            isEncrypting = true;
            promptForPassword();
        });

        btnDecrypt.setOnClickListener(v -> {
            isEncrypting = false;
            promptForPassword();
        });
    }

    private void selectFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_FILE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null && requestCode == PICK_FILE_REQUEST_CODE) {
            selectedFileUri = data.getData();
            if (selectedFileUri != null) {
                selectedFileName = getCleanFileName(selectedFileUri);
                if (selectedFileName != null) {
                    tvStatus.setText(getString(R.string.status_file_selected, selectedFileName));
                    btnEncrypt.setEnabled(true);
                    btnDecrypt.setEnabled(true);
                } else {
                    Toast.makeText(this, "Nome do arquivo não encontrado.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Erro ao selecionar o arquivo.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void promptForPassword() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isEncrypting ? "Digite uma senha para criptografia" : "Digite uma senha para descriptografia");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("Confirmar", (dialog, which) -> {
            String password = input.getText().toString();
            if (password.isEmpty()) {
                Toast.makeText(this, "Senha não pode estar vazia!", Toast.LENGTH_SHORT).show();
            } else {
                if (isEncrypting) {
                    encryptFile(password);
                } else {
                    decryptFile(password);
                }
            }
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void encryptFile(String password) {
        try {
            if (selectedFileUri == null) {
                Toast.makeText(this, "Nenhum arquivo selecionado.", Toast.LENGTH_SHORT).show();
                return;
            }

            InputStream inputStream = getContentResolver().openInputStream(selectedFileUri);
            if (inputStream == null) {
                Toast.makeText(this, "Erro ao abrir o arquivo para leitura.", Toast.LENGTH_SHORT).show();
                return;
            }

            Uri outputUri = createOutputFile(selectedFileName + ".enc");
            if (outputUri == null) {
                Toast.makeText(this, "Erro ao criar arquivo de saída.", Toast.LENGTH_SHORT).show();
                return;
            }

            OutputStream outputStream = getContentResolver().openOutputStream(outputUri);
            if (outputStream == null) {
                Toast.makeText(this, "Erro ao abrir o arquivo para gravação.", Toast.LENGTH_SHORT).show();
                return;
            }

            CryptoUtils.encryptFile(inputStream, outputStream, password);
            tvStatus.setText(getString(R.string.success_encrypt));
            Toast.makeText(this, "Arquivo criptografado com sucesso!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            tvStatus.setText(getString(R.string.error));
            e.printStackTrace();
            Toast.makeText(this, "Erro ao criptografar o arquivo!", Toast.LENGTH_SHORT).show();
        }
    }

    private void decryptFile(String password) {
        try {
            if (selectedFileUri == null) {
                Toast.makeText(this, "Nenhum arquivo selecionado.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!selectedFileName.endsWith(".enc")) {
                Toast.makeText(this, "Arquivo selecionado não é válido para descriptografia!", Toast.LENGTH_SHORT).show();
                return;
            }

            InputStream inputStream = getContentResolver().openInputStream(selectedFileUri);
            if (inputStream == null) {
                Toast.makeText(this, "Erro ao abrir o arquivo para leitura.", Toast.LENGTH_SHORT).show();
                return;
            }

            Uri outputUri = createOutputFile(selectedFileName.replace(".enc", ""));
            if (outputUri == null) {
                Toast.makeText(this, "Erro ao criar arquivo de saída.", Toast.LENGTH_SHORT).show();
                return;
            }

            OutputStream outputStream = getContentResolver().openOutputStream(outputUri);
            if (outputStream == null) {
                Toast.makeText(this, "Erro ao abrir o arquivo para gravação.", Toast.LENGTH_SHORT).show();
                return;
            }

            CryptoUtils.decryptFile(inputStream, outputStream, password);
            tvStatus.setText(getString(R.string.success_decrypt));
            Toast.makeText(this, "Arquivo descriptografado com sucesso!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            tvStatus.setText(getString(R.string.error));
            e.printStackTrace();
            Toast.makeText(this, "Erro ao descriptografar o arquivo!", Toast.LENGTH_SHORT).show();
        }
    }

    private Uri createOutputFile(String fileName) {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/EncryptedFiles");

            return getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getCleanFileName(Uri uri) {
        String fileName = null;

        // Tentar obter o nome do arquivo pelo ContentResolver
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            }
        }

        // Se não conseguir pelo ContentResolver, tentar diretamente pelo caminho do URI
        if (fileName == null) {
            fileName = uri.getLastPathSegment();
        }

        // Limpar prefixos indesejados, caso existam
        if (fileName != null) {
            fileName = fileName.replace("primary_", "")
                    .replace("primary_Download_EncryptedFiles_", "");
        }

        return fileName;
    }
}
