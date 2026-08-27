package br.com.acm.epali_docs.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.com.acm.epali_docs.service.DocumentoService;
import br.com.acm.epali_docs.service.SupabaseStorageService;

@RestController
@RequestMapping("/api/documentos")
public class DocumentoController {

    @Autowired
    private DocumentoService documentoService;

    @Autowired
    private SupabaseStorageService supabaseStorageService;

    @PostMapping("/enviar")
    public String receberDocumento(
            @RequestParam Map<String, String> todosOsDados, // Captura TODOS os campos de texto do HTML
            @RequestParam("assinatura") MultipartFile assinatura) {

        try {
            String tipoDocumento = todosOsDados.getOrDefault("tipo_documento", "imagem");
            
            // 1. Pega a unidade escolhida no site
            String unidade = todosOsDados.getOrDefault("unidade", "Sem_Unidade");
            
            String nomeTemplate = tipoDocumento.equals("menor") 
                    ? "template_menor.pdf" 
                    : "template_imagem.pdf";
            
            // Pega o nome dependendo da aba
            String pessoaReferencia = tipoDocumento.equals("menor") 
                    ? todosOsDados.get("nome_filho_menor") 
                    : todosOsDados.get("nome_responsavel_img");
                    
            if (pessoaReferencia == null || pessoaReferencia.isEmpty()) {
                pessoaReferencia = "Participante";
            }
            
            // Cria o nome do arquivo limpo (substituindo espaços por underline)
            String nomeArquivoFinal = "Autorizacao_" + tipoDocumento.toUpperCase() + "_" + pessoaReferencia.replaceAll("\\s+", "_") + ".pdf";

            // === 2. A MÁGICA DAS PASTAS ===
            // Define o nome da subpasta de acordo com o documento
            String subpasta = tipoDocumento.equals("imagem") ? "autorizacao_imagem" : "autorizacao_menor";
            
            // Junta tudo: Unidade / Subpasta / Nome_do_Arquivo.pdf
            String caminhoSupabase = unidade + "/" + subpasta + "/" + nomeArquivoFinal;

            // 3. Passa os dados de texto junto com a assinatura para o Serviço preencher tudo!
            byte[] pdfGerado = documentoService.preencherECarimbar(assinatura, nomeTemplate, todosOsDados);

            // 4. Envia para o Supabase usando o CAMINHO COMPLETO COM AS PASTAS
            String urlPublica = supabaseStorageService.enviarArquivo(pdfGerado, caminhoSupabase);

            // 5. NOVA TELA DE SUCESSO (Com o layout em cartão conectando no CSS externo)
            return "<!DOCTYPE html>\n" +
                   "<html lang=\"pt-BR\">\n" +
                   "<head>\n" +
                   "    <meta charset=\"UTF-8\">\n" +
                   "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                   "    <title>Sucesso - Portal EPALI</title>\n" +
                   "    <link rel=\"stylesheet\" href=\"/css/sucesso.css\">\n" +
                   "</head>\n" +
                   "<body>\n" +
                   "    <div class=\"success-card\">\n" +
                   "        <h1 class=\"success-title\">Sucesso!</h1>\n" +
                   "        <p class=\"success-message\">\n" +
                   "            O documento de <strong>" + pessoaReferencia + "</strong> foi assinado e salvo na nuvem.\n" +
                   "        </p>\n" +
                   "        <div class=\"success-path\">\n" +
                   "            Salvo na pasta: <strong>" + unidade + " &gt; " + subpasta + "</strong>\n" +
                   "        </div>\n" +
                   "        <p class=\"success-message\">Você já pode acessar o PDF gerado clicando no botão abaixo:</p>\n" +
                   "        <div class=\"button-group\">\n" +
                   "            <a href=\"" + urlPublica + "\" target=\"_blank\" class=\"btn btn-view\">Visualizar Documento</a>\n" +
                   "            <a href=\"/index.html\" class=\"btn btn-new\">Enviar novo documento</a>\n" +
                   "        </div>\n" +
                   "    </div>\n" +
                   "</body>\n" +
                   "</html>";

        } catch (Exception e) {
            e.printStackTrace();
            
            // VERIFICAÇÃO DE DOCUMENTO DUPLICADO (Erro 409)
            if (e.getMessage() != null && (e.getMessage().contains("409") || e.getMessage().contains("Duplicate") || e.getMessage().contains("KeyAlreadyExists"))) {
                return "<!DOCTYPE html>\n" +
                       "<html lang=\"pt-BR\">\n" +
                       "<head>\n" +
                       "    <meta charset=\"UTF-8\">\n" +
                       "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                       "    <title>Atenção - Portal EPALI</title>\n" +
                       "    <link rel=\"stylesheet\" href=\"/css/duplicado.css\">\n" +
                       "</head>\n" +
                       "<body>\n" +
                       "    <div class=\"warning-card\">\n" +
                       "        <h1 class=\"warning-title\">Atenção</h1>\n" +
                       "        <p class=\"warning-message\">\n" +
                       "            Um documento já foi preenchido para esse jovem, se achar necessário entre em contato com um membro da comissão.\n" +
                       "        </p>\n" +
                       "        <div class=\"button-group\">\n" +
                       "            <a href=\"/index.html\" class=\"btn btn-back\">Voltar para o Início</a>\n" +
                       "        </div>\n" +
                       "    </div>\n" +
                       "</body>\n" +
                       "</html>";
            }

            // TELA DE ERRO GENÉRICO (Caso dê um erro inesperado que não seja duplicado)
            return "<h1 style='color: red;'>Erro ao processar o documento:</h1><p>" + e.getMessage() + "</p>";
        }
    }
}