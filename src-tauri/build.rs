fn main() -> Result<(), Box<dyn std::error::Error>> {
    tonic_prost_build::configure()
        .build_server(false)
        .type_attribute(".", "#[derive(serde::Serialize,serde::Deserialize)]") // Add serde::Serialize to all message types and enums
        .compile_well_known_types(true)
        .compile_protos(
            &["../proto/protocol.proto"],
            &["../proto"], // specify the root location to search proto dependencies
        )?;

    tauri_build::build();

    Ok(())
}
