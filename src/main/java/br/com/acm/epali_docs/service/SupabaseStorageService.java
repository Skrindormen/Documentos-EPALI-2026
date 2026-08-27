package br.com.acm.epali_docs.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SupabaseStorageService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    @Value("${supabase.bucket}")
    private String bucketName;

    public String enviarArquivo(byte[] arquivoBytes, String nomeArquivo) {
        try {
            // URL do endpoint de Storage do Supabase para upload de arquivos
            String url = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + nomeArquivo;

            RestTemplate restTemplate = new RestTemplate();

            // Configura os cabeçalhos de autenticação exigidos pelo Supabase
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.set("Authorization", "Bearer " + supabaseKey);
            headers.set("apikey", supabaseKey);

            HttpEntity<byte[]> requestEntity = new HttpEntity<>(arquivoBytes, headers);

            // Faz a requisição POST enviando os bytes do PDF
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

            if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) {
                // Retorna a URL pública direta onde o documento pode ser acessado depois
                return supabaseUrl + "/storage/v1/object/public/" + bucketName + "/" + nomeArquivo;
            } else {
                throw new RuntimeException("Erro ao enviar para o Supabase: " + response.getBody());
            }

        } catch (Exception e) {
            throw new RuntimeException("Falha na integração com Supabase: " + e.getMessage(), e);
        }
    }
    
    public List<Map<String, Object>> buscarTodosDocumentos() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + supabaseKey);
            headers.set("apikey", supabaseKey);
            headers.set("Content-Type", "application/json");

            // O parâmetro "search" faz o Supabase procurar em TODAS as subpastas automaticamente
            // A API exige o parâmetro "prefix", mesmo que vazio, junto com o "search"
            String body = "{\"prefix\": \"\", \"search\": \"Autorizacao_\"}";
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            // CORREÇÃO AQUI: Trocado supabaseBucket por bucketName
            String urlList = supabaseUrl + "/storage/v1/object/list/" + bucketName;

            // Faz a chamada e recebe uma lista de objetos do Supabase
            ResponseEntity<List> response = restTemplate.exchange(
                    urlList,
                    HttpMethod.POST,
                    entity,
                    List.class
            );

            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}