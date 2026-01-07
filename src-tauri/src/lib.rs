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

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_opener::init())
        .invoke_handler(tauri::generate_handler![greet])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
