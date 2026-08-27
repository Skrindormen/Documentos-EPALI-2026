package br.com.acm.epali_docs.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

@Service
public class DocumentoService {

    public byte[] preencherECarimbar(MultipartFile arquivoEnviado, String nomeTemplate, Map<String, String> dados) throws Exception {
        
        System.out.println("====== NOMES DOS CAMPOS RECEBIDOS DO HTML ======");
        System.out.println(dados.keySet());
        System.out.println("================================================");

        InputStream templateStream = getClass().getResourceAsStream("/pdfs/" + nomeTemplate);
        if (templateStream == null) {
            throw new RuntimeException("Template PDF não encontrado: " + nomeTemplate);
        }

        PDDocument documento = PDDocument.load(templateStream);
        PDPage pagina = documento.getPage(0); 

        PDPageContentStream contentStream = new PDPageContentStream(
                documento, pagina, PDPageContentStream.AppendMode.APPEND, true, true);
        
        contentStream.setFont(PDType1Font.HELVETICA, 12);

        // Variável dinâmica para controlar a altura da assinatura
        float assinaturaY;
        boolean isTemplateMenor = nomeTemplate.equals("template_menor.pdf");

       if (isTemplateMenor) {
            // ==========================================
            // BATALHA NAVAL: DOCUMENTO DE MENOR (Ajuste Fino)
            // ==========================================

            // 2. DATA (São Paulo...)
            LocalDate hoje = LocalDate.now();
            String dia = String.format("%02d", hoje.getDayOfMonth());
            String mes = hoje.format(DateTimeFormatter.ofPattern("MMMM", new Locale("pt", "BR")));
            String ano = String.valueOf(hoje.getYear()).substring(2); 

            // Ajustando as posições horizontais (X) do dia e do ano
            escreverTexto(contentStream, dia, 180, 660); // Jogamos o dia mais para a direita
            escreverTexto(contentStream, mes, 330, 660); // O mês já estava perfeito
            escreverTexto(contentStream, ano, 485, 660); // Puxamos o ano para a esquerda

            // 3. NOME DO RESPONSÁVEL
            String nomeResponsavelMenor = dados.get("nome_responsavel_menor");
            // Subimos o Y para 625 para desencostar da linha preta
            escreverTexto(contentStream, nomeResponsavelMenor, 240, 625); 

            // 4. ASSINATURA (Subimos de 420 para 470 para aproximar da linha)
            assinaturaY = 470; 

        } else {
            // ==========================================
            // COORDENADAS: DOCUMENTO DE IMAGEM
            // ==========================================

            escreverTexto(contentStream, dados.get("nome_responsavel_img"), 85, 760); 
            escreverTexto(contentStream, dados.get("rg"), 105, 725);
            escreverTexto(contentStream, dados.get("cpf"), 295, 725);
            
            String filho = dados.getOrDefault("nome_filho_img", dados.get("nome_filho_menor"));
            escreverTexto(contentStream, filho, 210, 695);

            String dataRegistro = dados.get("data_registro");
            if (dataRegistro != null && dataRegistro.contains("/")) {
                String[] partes = dataRegistro.split("/");
                escreverTexto(contentStream, partes[0], 495, 622); 
                escreverTexto(contentStream, partes[1], 520, 622); 
                escreverTexto(contentStream, partes[2], 545, 622); 
            } else {
                escreverTexto(contentStream, dataRegistro, 495, 622);
            }

            escreverTexto(contentStream, dados.get("tel_residencial"), 185, 555); 
            escreverTexto(contentStream, dados.get("tel_celular"), 155, 525); 

            LocalDate hoje = LocalDate.now();
            String dia = String.format("%02d", hoje.getDayOfMonth());
            String mes = hoje.format(DateTimeFormatter.ofPattern("MMMM", new Locale("pt", "BR")));
            String ano = String.valueOf(hoje.getYear()).substring(2); 

            escreverTexto(contentStream, dia, 175, 206); 
            escreverTexto(contentStream, mes, 265, 206); 
            escreverTexto(contentStream, ano, 470, 206); 

            // Define a altura da assinatura para o documento de Imagem
            assinaturaY = 100;
        }

        // === 3. ASSINATURA ===
        PDImageXObject imagemAssinatura;
        String contentType = arquivoEnviado.getContentType();

        if (contentType != null && contentType.equals("application/pdf")) {
            PDDocument pdfAssinatura = PDDocument.load(arquivoEnviado.getInputStream());
            PDFRenderer pdfRenderer = new PDFRenderer(pdfAssinatura);
            BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(0, 150); 
            pdfAssinatura.close();
            imagemAssinatura = PDImageXObject.createFromByteArray(documento, convertToBytes(bufferedImage), "assinatura_convertida");
        } else {
            imagemAssinatura = PDImageXObject.createFromByteArray(documento, arquivoEnviado.getBytes(), arquivoEnviado.getOriginalFilename());
        }

        float larguraDesejada = 150f;
        float proporcao = (float) imagemAssinatura.getHeight() / imagemAssinatura.getWidth();
        float alturaCalculada = larguraDesejada * proporcao;

        // Desenha a assinatura usando a variável assinaturaY que se adapta ao documento
        contentStream.drawImage(imagemAssinatura, 220, assinaturaY, larguraDesejada, alturaCalculada);
        
        contentStream.close();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        documento.save(outputStream);
        documento.close();

        return outputStream.toByteArray();
    }

    private void escreverTexto(PDPageContentStream contentStream, String texto, float x, float y) {
        if (texto == null || texto.trim().isEmpty()) return;
        try {
            contentStream.beginText();
            contentStream.newLineAtOffset(x, y);
            contentStream.showText(texto);
            contentStream.endText();
        } catch (Exception e) {
            System.out.println("Erro ao escrever texto no PDF");
        }
    }

    private byte[] convertToBytes(BufferedImage image) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }
}