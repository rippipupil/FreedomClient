fn main() {
    // El commit de GitHub Actions queda dentro del .exe para que el launcher sepa si hay una versión nueva.
    println!("cargo:rerun-if-env-changed=GITHUB_SHA");
    println!("cargo:rustc-env=FC_BUILD_COMMIT={}", std::env::var("GITHUB_SHA").unwrap_or_default());
    tauri_build::build()
}
