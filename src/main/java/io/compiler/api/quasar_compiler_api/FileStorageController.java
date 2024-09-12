package io.compiler.api.quasar_compiler_api;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.compiler.core.QuasarGrammarLexer;
import io.compiler.core.QuasarGrammarParser;
import io.compiler.core.ast.Program;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("api/files")
@Tag(name = "api/files")
public class FileStorageController {

    private final Path fileStorageLocation;

    public FileStorageController(FileStorageProperties fileStorageProperties) {
        this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir())
            .toAbsolutePath().normalize();
    }

    @Operation(description = "Realiza o upload de arquivos", method = "POST")
    @ApiResponse(responseCode = "200", description = "Download realizado com sucesso")
    @ApiResponse(responseCode = "500", description = "Erro ao realizar o download")
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileNameWithoutExtension = StringUtils.stripFilenameExtension(originalFileName);

        try {
            Path targetLocation = fileStorageLocation.resolve(originalFileName);
            file.transferTo(targetLocation);

            // Processar o arquivo com ANTLR
            try {
                QuasarGrammarLexer lexer;
                QuasarGrammarParser parser;

                // Criar analisador léxico a partir do arquivo recebido
                lexer = new QuasarGrammarLexer(CharStreams.fromPath(targetLocation));

                // Fluxo de tokens
                CommonTokenStream tokenStream = new CommonTokenStream(lexer);

                // Criar parser a partir do fluxo de tokens
                parser = new QuasarGrammarParser(tokenStream);

                System.out.println("(+) Quasar Compiler");
                parser.program();
                System.out.println("(+) Compilation Successfully");

                // Geração de código
                Program program = parser.getProgram();
                program.setUnusedVarWarning(true);
                program.setName(fileNameWithoutExtension);

                program.verifyUnusedVar();

                String generatedCode = program.generateTarget();
                System.out.println(generatedCode);

                // Salvar o código gerado em um arquivo
                Path generatedFilePath = fileStorageLocation.resolve(fileNameWithoutExtension + ".java");
                Files.write(generatedFilePath, generatedCode.getBytes());

                String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/files/download/")
                    .path(fileNameWithoutExtension + ".java")
                    .toUriString();

                return ResponseEntity.ok("Subiu! Download do arquivo: " + fileDownloadUri);

            } catch (Exception ex) {
                System.err.println("(-) Error: " + ex.getMessage());
                ex.printStackTrace();
                return ResponseEntity.status(500).body("Erro ao processar o arquivo: " + ex.getMessage());
            }

        } catch (IOException ex) {
            return ResponseEntity.badRequest().build();
        }
    }


    @Operation(description = "Realiza o download de arquivos", method = "GET")
    @ApiResponse(responseCode = "200", description = "Download realizado com sucesso")
    @ApiResponse(responseCode = "500", description = "Erro ao realizar o download")
    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest request) 
        throws IOException {
        Path filePath = fileStorageLocation.resolve(fileName).normalize();

        try {
            Resource resource = new UrlResource(filePath.toUri());
            String contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());

            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(description = "Realiza a listagem de arquivos enviados e gerados", method = "GET")
    @ApiResponse(responseCode = "200", description = "Listagem realizada com sucesso")
    @ApiResponse(responseCode = "500", description = "Erro ao realizar a listagem")
    @GetMapping("/list")
    public ResponseEntity<List<String>> listFiles() {
        try {
            List<String> fileNames = Files.list(fileStorageLocation)
                .map(Path::getFileName)
                .map(Path::toString)
                .collect(Collectors.toList());

            return ResponseEntity.ok(fileNames);
        } catch (IOException e) {
            return ResponseEntity.status(500).build();
        }
    }
}
