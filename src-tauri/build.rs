fn main() -> Result<(), Box<dyn std::error::Error>> {
    tonic_prost_build::configure()
        .build_server(false)
        .out_dir("src/proto") // you can change the generated code's location
        .compile_protos(
            &["../proto/protocol.proto"],
            &["../proto"], // specify the root location to search proto dependencies
        )?;

    tauri_build::build();

    Ok(())
}
