fn main() -> Result<(), Box<dyn std::error::Error>> {
    tonic_prost_build::configure()
        .build_server(false)
        .out_dir("src/proto") // you can change the generated code's location
        .type_attribute(".", "#[derive(serde::Serialize)]") // Add serde::Serialize to all message types and enums
        .compile_protos(
            &["../proto/protocol.proto"],
            &["../proto"], // specify the root location to search proto dependencies
        )?;

    tauri_build::build();

    Ok(())
}
