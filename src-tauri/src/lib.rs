use tokio::sync::Mutex;
use std::sync::Arc;
use tonic::transport::Channel;
use Jmvram::app_backend_client::AppBackendClient;
use tauri::State;

#[derive(Debug, thiserror::Error)]
enum Error {
    #[error(transparent)]
    Io(#[from] std::io::Error),
    #[error("failed to parse as string: {0}")]
    Utf8(#[from] std::str::Utf8Error),
    #[error(transparent)]
    InvalidUri(#[from] tonic::transport::Error),
    #[error(transparent)]
    GrpcStatus(#[from] tonic::Status),
}

#[derive(serde::Serialize)]
#[serde(tag = "kind", content = "message")]
#[serde(rename_all = "camelCase")]
enum ErrorKind {
    Io(String),
    Utf8(String),
    InvalidUri(String),
    GrpcStatus(String),
}

impl serde::Serialize for Error {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: serde::ser::Serializer,
    {
        let error_message = self.to_string();
        let error_kind = match self {
            Self::Io(_) => ErrorKind::Io(error_message),
            Self::Utf8(_) => ErrorKind::Utf8(error_message),
            Self::InvalidUri(_) => ErrorKind::InvalidUri(error_message),
            Self::GrpcStatus(_) => ErrorKind::GrpcStatus(error_message),
        };
        error_kind.serialize(serializer)
    }
}

const GRPC_SERVER_ADDRESS: &str = "localhost:53333";

async fn create_grpc_client() -> AppBackendClient<Channel> {
    let channel = Channel::from_static(&GRPC_SERVER_ADDRESS)
        .connect()
        .await
        .expect(&format!(
            "Can't create gRPC channel for a port {}",
            GRPC_SERVER_ADDRESS
        ));

    AppBackendClient::new(channel)
}

fn sync_create_grpc_client() -> AppBackendClient<Channel> {
    let handle = tokio::runtime::Handle::try_current().unwrap_or_else(|_| {
        tokio::runtime::Runtime::new()
            .expect("Failed to create runtime")
            .handle()
            .clone()
    });
    let client = handle.block_on(create_grpc_client());
    client
}

struct AppState {
    client: AppBackendClient<Channel>,
}

impl AppState {
    fn new() -> Self {
        let client = sync_create_grpc_client();
        Self { client }
    }
}

// Модуль google должен быть доступен через super:: из сгенерированного jvmram.rs
// Сгенерированный код находится в модуле jvmram (по имени package в proto)
pub mod google {
    pub mod protobuf {
        include!(concat!(env!("OUT_DIR"), "/google.protobuf.rs"));
    }
}

// Включаем сгенерированный код в модуль с именем пакета из proto (jvmram)
pub mod jvmram {
    include!(concat!(env!("OUT_DIR"), "/jvmram.rs"));
}

// Реэкспорт для удобства использования с правильным именем
pub mod Jmvram {
    pub use crate::jvmram::*;
}

// Learn more about Tauri commands at https://tauri.app/develop/calling-rust/
#[tauri::command]
fn greet(name: &str) -> String {
    format!("Hello, {}! You've been greeted from Rust!", name)
}

async fn extract_client(state: State<'_, Arc<Mutex<AppState>>>) -> Result<AppBackendClient<Channel>, Error> {
    let cloned = Arc::clone(&state);
    let lock = cloned
            .try_lock()
            .map_err(|_| String::from("cannot change state while it is being used"))
            .unwrap();
    let client = lock.client.to_owned();
    Ok(client)
}

#[tauri::command]
async fn set_visible(state: State<'_, Arc<Mutex<AppState>>>, request: Jmvram::SetVisibleRequest) -> Result<(), Error> {
    let mut client = extract_client(state).await?;
    client.set_visible(request).await?;
    Ok(())
}

#[tauri::command]
async fn set_invisible(state: State<'_, Arc<Mutex<AppState>>>, request: Jmvram::SetInvisibleRequest) -> Result<(), Error> {
    let mut client = extract_client(state).await?;
    client.set_invisible(request).await?;
    Ok(())
}

use tauri::{Builder, Window, WindowEvent, Manager};

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    Builder::default()
        .setup(|app| {
            let state = AppState::new();
            let guarded_state = Mutex::new(state);
            app.manage(guarded_state);
            Ok(())
        })
        .plugin(tauri_plugin_opener::init())
        .invoke_handler(tauri::generate_handler![greet, set_visible, set_invisible])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
