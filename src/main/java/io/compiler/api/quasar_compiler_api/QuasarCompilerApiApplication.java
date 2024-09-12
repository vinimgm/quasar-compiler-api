package io.compiler.api.quasar_compiler_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "Quasar Compiler API", version = "1", description = "API criada para compilar arquivos da QuasarLanguage"))
public class QuasarCompilerApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(QuasarCompilerApiApplication.class, args);
	}

}
