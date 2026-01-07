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

use Jmvram::app_backend_client::AppBackendClient;

const GRPC_SERVER_ADDRESS: &str = "localhost:53333";

async fn create_grpc_client() -> AppBackendClient<tonic::transport::Channel> {
    let channel = tonic::transport::Channel::from_static(&GRPC_SERVER_ADDRESS)
        .connect()
        .await
        .expect("Can't create a channel");

    AppBackendClient::new(channel)
}

/* 
fn convert_serde_to_prost(value: serde_json::Value) -> prost_types::Value {
    if value.is_array() {
        let mut vector: Vec<prost_types::Value> = vec![];
        for val in value.as_array().unwrap().clone() {
            vector.push(convert_serde_to_prost(val));
        }
        prost_types::Value {
            kind: Some(Kind::ListValue(prost_types::ListValue {
                values: vector.clone(),
            })),
        }
    } else if value.is_boolean() {
        prost_types::Value {
            kind: Some(Kind::BoolValue(value.as_bool().unwrap().clone())),
        }
    } else if value.is_f64() {
        prost_types::Value {
            kind: Some(Kind::NumberValue(value.as_f64().unwrap().clone())),
        }
    } else if value.is_i64() {
        prost_types::Value {
            kind: Some(Kind::NumberValue(value.as_i64().unwrap() as f64)),
        }
    } else if value.is_null() {
        prost_types::Value {
            kind: Some(Kind::NullValue(0)),
        }
    } else if value.is_string() {
        prost_types::Value {
            kind: Some(Kind::StringValue(value.as_str().unwrap().to_string())),
        }
    } else if value.is_object() {
        let mut btree: BTreeMap<String, prost_types::Value> = BTreeMap::new();
        for (key, val) in value.as_object().unwrap().clone() {
            btree.insert(key.as_str().to_string(), convert_serde_to_prost(val));
        }
        prost_types::Value {
            kind: Some(Kind::StructValue(prost_types::Struct {
                fields: btree.clone(),
            })),
        }
    } else {
        panic!("Unknown value type")
    }
}
*/

#[tauri::command]
async fn set_visible(request: Jmvram::SetVisibleRequest) -> Result<(), Error> {
    let mut client = create_grpc_client().await;
    client.set_visible(request).await?;
    Ok(())
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_opener::init())
        .invoke_handler(tauri::generate_handler![greet, set_visible])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
