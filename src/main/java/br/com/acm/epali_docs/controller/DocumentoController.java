package br.com.acm.epali_docs.controller;

import br.com.acm.epali_docs.service.DocumentoService;
import br.com.acm.epali_docs.service.SupabaseStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

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
            // Exemplo: Guarulhos/autorizacao_menor/Autorizacao_MENOR_heitor.pdf
            String caminhoSupabase = unidade + "/" + subpasta + "/" + nomeArquivoFinal;

            // 3. Passa os dados de texto junto com a assinatura para o Serviço preencher tudo!
            byte[] pdfGerado = documentoService.preencherECarimbar(assinatura, nomeTemplate, todosOsDados);

            // 4. Envia para o Supabase usando o CAMINHO COMPLETO COM AS PASTAS
            String urlPublica = supabaseStorageService.enviarArquivo(pdfGerado, caminhoSupabase);

            // 5. Retorna a tela de sucesso (adicionei um texto mostrando a pasta que foi salva)
            return "<div style='font-family: Arial; text-align: center; margin-top: 50px;'>" +
                   "<h1 style='color: #0033A0;'>Sucesso!</h1>" +
                   "<p>O documento de <b>" + pessoaReferencia + "</b> foi assinado e salvo na nuvem da ACM.</p>" +
                   "<p style='color: #666; font-size: 14px;'>Salvo na pasta: <b>" + unidade + " &gt; " + subpasta + "</b></p>" +
                   "<p>Você já pode acessar o PDF gerado clicando no botão abaixo:</p>" +
                   "<a href='" + urlPublica + "' target='_blank' style='display: inline-block; margin-top: 20px; margin-right: 10px; padding: 10px 20px; background: #009688; color: white; text-decoration: none; border-radius: 5px;'>Visualizar Documento</a>" +
                   "<a href='/index.html' style='display: inline-block; margin-top: 20px; padding: 10px 20px; background: #E31837; color: white; text-decoration: none; border-radius: 5px;'>Enviar novo documento</a>" +
                   "</div>";

        } catch (Exception e) {
            e.printStackTrace();
            return "<h1 style='color: red;'>Erro ao processar o documento:</h1><p>" + e.getMessage() + "</p>";
        }
    }
}