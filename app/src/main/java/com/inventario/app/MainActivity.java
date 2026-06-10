package com.inventario.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private InventoryDbHelper db;
    private InventoryAdapter adapter;

    private TextView tvCurrentRoom;
    private TextView tvCount;

    // Sala activa: los objetos escaneados se asignan a esta sala.
    private String currentRoom = null;

    // Qué estamos escaneando ahora: SALA u OBJETO (lo elige el botón pulsado).
    private enum Mode { ROOM, OBJECT }
    private Mode pendingMode = Mode.OBJECT;

    private final SimpleDateFormat ts =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    // Lanzador del escáner de ZXing.
    private final ActivityResultLauncher<ScanOptions> scanLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) {
                    handleScan(result.getContents().trim(), pendingMode);
                }
            });

    // Lanzador para pedir permiso de cámara.
    private final ActivityResultLauncher<String> cameraPermLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (granted) launchScanner();
                        else Toast.makeText(this, "Se necesita permiso de cámara para escanear",
                                Toast.LENGTH_LONG).show();
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new InventoryDbHelper(this);

        tvCurrentRoom = findViewById(R.id.tvCurrentRoom);
        tvCount = findViewById(R.id.tvCount);

        Button btnScanRoom = findViewById(R.id.btnScanRoom);
        Button btnScanObject = findViewById(R.id.btnScanObject);
        Button btnExport = findViewById(R.id.btnExport);
        Button btnClear = findViewById(R.id.btnClear);

        RecyclerView rv = findViewById(R.id.recycler);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InventoryAdapter();
        rv.setAdapter(adapter);

        btnScanRoom.setOnClickListener(v -> { pendingMode = Mode.ROOM; checkPermissionAndScan(); });
        btnScanObject.setOnClickListener(v -> { pendingMode = Mode.OBJECT; checkPermissionAndScan(); });
        btnExport.setOnClickListener(v -> exportCsv());
        btnClear.setOnClickListener(v -> confirmClear());

        updateCurrentRoomLabel();
        refreshList();
    }

    private void checkPermissionAndScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchScanner();
        } else {
            cameraPermLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchScanner() {
        ScanOptions options = new ScanOptions();
        options.setPrompt(pendingMode == Mode.ROOM
                ? "Escanea el código de la SALA"
                : "Escanea el código del OBJETO");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        // Acepta tanto códigos de barras 1D (Code 39, Code 128, etc.) como QR.
        options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES);
        scanLauncher.launch(options);
    }

    /** Según el botón pulsado, trata el código como SALA o como OBJETO. */
    private void handleScan(String code, Mode mode) {
        if (TextUtils.isEmpty(code)) return;
        if (mode == Mode.ROOM) {
            currentRoom = code;
            updateCurrentRoomLabel();
            Toast.makeText(this, "Sala activa: " + code, Toast.LENGTH_SHORT).show();
        } else {
            handleObjectScan(code);
        }
    }

    private void handleObjectScan(String objectCode) {
        if (currentRoom == null) {
            new AlertDialog.Builder(this)
                    .setTitle("Sin sala activa")
                    .setMessage("Primero pulsa \"ESCANEAR SALA\" y lee el código de la sala "
                            + "para indicar dónde está el objeto.")
                    .setPositiveButton("Entendido", null)
                    .show();
            return;
        }

        String now = ts.format(new Date());
        InventoryItem existing = db.findByObject(objectCode);

        if (existing == null) {
            // Objeto nuevo -> se inserta relacionado con la sala activa.
            db.insertObject(objectCode, currentRoom, now);
            Toast.makeText(this,
                    "Nuevo objeto registrado en sala " + currentRoom,
                    Toast.LENGTH_SHORT).show();
            refreshList();
        } else {
            // El objeto YA existe en la tabla: avisamos y ofrecemos actualizar la sala.
            String msg = "El objeto \"" + objectCode + "\" ya está registrado.\n\n"
                    + "Sala actual en BD: " + existing.roomCode + "\n"
                    + "Primer escaneo: " + existing.firstSeen + "\n"
                    + "Último escaneo: " + existing.lastSeen + "\n"
                    + "Veces escaneado: " + existing.scanCount + "\n\n"
                    + "¿Mover a la sala activa (" + currentRoom + ")?";
            new AlertDialog.Builder(this)
                    .setTitle("Objeto duplicado")
                    .setMessage(msg)
                    .setPositiveButton("Actualizar sala", (d, w) -> {
                        db.updateObjectRoom(objectCode, currentRoom, now);
                        Toast.makeText(this, "Sala actualizada", Toast.LENGTH_SHORT).show();
                        refreshList();
                    })
                    .setNegativeButton("Mantener", null)
                    .show();
        }
    }

    private void updateCurrentRoomLabel() {
        tvCurrentRoom.setText(currentRoom == null
                ? "Sala activa: (ninguna — escanea una sala)"
                : "Sala activa: " + currentRoom);
    }

    private void refreshList() {
        List<InventoryItem> all = db.getAll();
        adapter.setItems(all);
        tvCount.setText("Objetos registrados: " + all.size());
    }

    private void confirmClear() {
        new AlertDialog.Builder(this)
                .setTitle("Borrar todo")
                .setMessage("¿Eliminar todos los registros de inventario? Esta acción no se puede deshacer.")
                .setPositiveButton("Borrar", (d, w) -> {
                    db.deleteAll();
                    refreshList();
                    Toast.makeText(this, "Inventario vaciado", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /** Exporta la tabla a un CSV y abre el selector para compartirlo. */
    private void exportCsv() {
        List<InventoryItem> all = db.getAll();
        if (all.isEmpty()) {
            Toast.makeText(this, "No hay datos para exportar", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            File dir = new File(getExternalFilesDir(null), "exports");
            if (!dir.exists()) dir.mkdirs();
            String fileName = "inventario_" + new SimpleDateFormat("yyyyMMdd_HHmmss",
                    Locale.getDefault()).format(new Date()) + ".csv";
            File file = new File(dir, fileName);

            FileWriter w = new FileWriter(file);
            w.append("object_code,room_code,first_seen,last_seen,scan_count\n");
            for (InventoryItem it : all) {
                w.append(csv(it.objectCode)).append(',')
                        .append(csv(it.roomCode)).append(',')
                        .append(csv(it.firstSeen)).append(',')
                        .append(csv(it.lastSeen)).append(',')
                        .append(String.valueOf(it.scanCount)).append('\n');
            }
            w.flush();
            w.close();

            Uri uri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", file);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/csv");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, "Exportar inventario CSV"));
        } catch (Exception e) {
            Toast.makeText(this, "Error al exportar: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    /** Escapa un valor para CSV. */
    private String csv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
