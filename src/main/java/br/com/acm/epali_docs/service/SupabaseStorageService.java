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
import java.util.ArrayList;

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
        // Criamos uma lista vazia que vai acumular os PDFs de todas as unidades
        List<Map<String, Object>> todosDocumentos = new ArrayList<>();
        
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + supabaseKey);
            headers.set("apikey", supabaseKey);
            headers.set("Content-Type", "application/json");

            String urlList = supabaseUrl + "/storage/v1/object/list/" + bucketName;

            // Lista de todas as unidades da ACM
            String[] unidades = {
                "Alphaville", "Centro", "Guarulhos", "Itaquera", "Lapa", 
                "Norte", "Osasco", "Ribeirao_Preto", "Santo_Amaro", "Sao_Jose_dos_Campos"
            };
            String[] subpastas = {"autorizacao_imagem", "autorizacao_menor"};

            // O Java vai bater rapidamente na porta de cada pastinha específica e recolher os PDFs
            for (String unidade : unidades) {
                for (String subpasta : subpastas) {
                    String prefixo = unidade + "/" + subpasta;
                    
                    // Agora pesquisamos DENTRO da subpasta exata
                    String body = "{\"prefix\": \"" + prefixo + "\", \"search\": \"Autorizacao_\"}";
                    HttpEntity<String> entity = new HttpEntity<>(body, headers);

                    try {
                        ResponseEntity<List> response = restTemplate.exchange(urlList, HttpMethod.POST, entity, List.class);
                        List<Map<String, Object>> arquivos = response.getBody();

                        if (arquivos != null) {
                            for (Map<String, Object> arquivo : arquivos) {
                                String nomeOriginal = (String) arquivo.get("name");
                                
                                // O Supabase devolve só o nome "Autorizacao.pdf". 
                                // Nós colamos o caminho da unidade antes para o seu HTML conseguir ler a unidade certa!
                                if (nomeOriginal != null && nomeOriginal.endsWith(".pdf")) {
                                    arquivo.put("name", prefixo + "/" + nomeOriginal);
                                    todosDocumentos.add(arquivo);
                                }
                            }
                        }
                    } catch (Exception e) {
                        // Se a pasta ainda não existir ou estiver vazia, ele ignora silenciosamente
                        System.out.println("Nenhum arquivo na pasta: " + prefixo);
                    }
                }
            }
            
            return todosDocumentos; // Retorna o "pacotão" cheio de arquivos
            
        } catch (Exception e) {
            e.printStackTrace();
            return todosDocumentos; // Retorna a lista vazia (em vez de null) para o seu JavaScript não travar
        }
    }
}