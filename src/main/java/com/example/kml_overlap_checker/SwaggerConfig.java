package com.example.kml_overlap_checker;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
// import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
// import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
    info = @Info(
        title = "KML Overlap Checker API",
        version = "1.0",
        description = "Detects partial overlaps between KML files using JTS and JAK"
    ),
    servers = @Server(url = "http://localhost:8080")
)
@Configuration
public class SwaggerConfig {
    // No beans required unless you want advanced customization
}