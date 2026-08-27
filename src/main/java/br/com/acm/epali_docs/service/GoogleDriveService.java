package br.com.acm.epali_docs.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Collections;

@Service
public class GoogleDriveService {

    // O ID da sua pasta no Google Drive
    private static final String FOLDER_ID = "1kTqz7bE_fSgOrgY_FLZ9QCN3pMHM_0eI";

    public String uploadArquivo(byte[] arquivoBytes, String nomeArquivo) throws Exception {
        
        // 1. Pega o arquivo de credenciais json
        InputStream in = getClass().getResourceAsStream("/credenciais.json");
        if (in == null) {
            throw new RuntimeException("Arquivo credenciais.json não encontrado!");
        }

        @SuppressWarnings("deprecation")
        GoogleCredential credential = GoogleCredential.fromStream(in)
                .createScoped(Collections.singleton(DriveScopes.DRIVE));

        // 2. Constrói o serviço do Google Drive
        Drive driveService = new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                credential)
                .setApplicationName("EPALI Assinaturas")
                .build();

        // 3. Configura os metadados do arquivo e define a pasta pai
        File fileMetadata = new File();
        fileMetadata.setName(nomeArquivo);
        fileMetadata.setParents(Collections.singletonList(FOLDER_ID));

        // 4. Usa ByteArrayContent (Upload Simples) para contornar a limitação de cota de Service Accounts
        ByteArrayContent mediaContent = new ByteArrayContent("application/pdf", arquivoBytes);

        // 5. Executa o envio para o Google Drive
        File file = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id, webViewLink")
                .execute();

        System.out.println("Arquivo enviado com sucesso para o Drive! ID: " + file.getId());
        
        return file.getId();
    }
}