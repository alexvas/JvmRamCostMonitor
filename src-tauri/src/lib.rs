use tokio::sync::OnceCell;
use std::sync::Arc;
use tonic::transport::Channel;
use Jmvram::app_backend_client::AppBackendClient;
use tauri::{State, http::Uri};

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

const LOCALHOST_V4: IpAddr = IpAddr::V4(Ipv4Addr::new(127, 0, 0, 1));
const GRPC_SERVER_PORT: u16 = 53333;

use std::net::{IpAddr, Ipv4Addr};

fn create_grpc_client() -> AppBackendClient<Channel> {
    let uri = format!("http://{}:{}", LOCALHOST_V4, GRPC_SERVER_PORT)
        .parse::<Uri>()
        .expect("Invalid URI");
    let endpoint = tonic::transport::Endpoint::from(uri);
    let channel = endpoint.connect_lazy();
    AppBackendClient::new(channel)
}

struct AppState {
    client: OnceCell<AppBackendClient<Channel>>,
}

impl AppState {
    fn new() -> Self {
        Self { 
            client: OnceCell::new() 
        }
    }

    async fn get_client(&self) -> AppBackendClient<Channel> {
        self.client.get_or_init(|| async {
            create_grpc_client()
        }).await.clone()
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
#[allow(non_snake_case)]
pub mod Jmvram {
    pub use crate::jvmram::*;
}

async fn get_client(state: &State<'_, Arc<AppState>>) -> AppBackendClient<Channel> {
    state.get_client().await
}

#[tauri::command]
async fn set_visible(state: State<'_, Arc<AppState>>, request: Jmvram::SetVisibleRequest) -> Result<(), Error> {
    let mut client = get_client(&state).await;
    client.set_visible(request).await?;
    Ok(())
}

#[tauri::command]
async fn set_invisible(state: State<'_, Arc<AppState>>, request: Jmvram::SetInvisibleRequest) -> Result<(), Error> {
    let mut client = get_client(&state).await;
    client.set_invisible(request).await?;
    Ok(())
}


#[tauri::command]
async fn set_following_pids(state: State<'_, Arc<AppState>>, request: Jmvram::PidList) -> Result<(), Error> {
    let mut client = get_client(&state).await;
    client.set_following_pids(request).await?;
    Ok(())
}

use tauri::{Builder, Manager};

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    Builder::default()
        .setup(|app| {
            let state = Arc::new(AppState::new());
            app.manage(state);
            Ok(())
        })
        .plugin(tauri_plugin_opener::init())
        .invoke_handler(tauri::generate_handler![set_visible, set_invisible, set_following_pids])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
