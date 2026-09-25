/// RAM total del PC en MB (0 si no se puede saber).
pub fn total_memory_mb() -> u64 {
    imp::total_memory_mb()
}

#[cfg(windows)]
mod imp {
    use windows_sys::Win32::System::SystemInformation::{GlobalMemoryStatusEx, MEMORYSTATUSEX};

    pub fn total_memory_mb() -> u64 {
        let mut status: MEMORYSTATUSEX = unsafe { std::mem::zeroed() };
        status.dwLength = std::mem::size_of::<MEMORYSTATUSEX>() as u32;
        if unsafe { GlobalMemoryStatusEx(&mut status) } != 0 { status.ullTotalPhys / 1024 / 1024 } else { 0 }
    }
}

#[cfg(target_os = "linux")]
mod imp {
    pub fn total_memory_mb() -> u64 {
        std::fs::read_to_string("/proc/meminfo")
            .ok()
            .and_then(|s| {
                s.lines()
                    .find(|l| l.starts_with("MemTotal:"))
                    .and_then(|l| l.split_whitespace().nth(1))
                    .and_then(|kb| kb.parse::<u64>().ok())
            })
            .map(|kb| kb / 1024)
            .unwrap_or(0)
    }
}

#[cfg(target_os = "macos")]
mod imp {
    pub fn total_memory_mb() -> u64 {
        std::process::Command::new("sysctl")
            .args(["-n", "hw.memsize"])
            .output()
            .ok()
            .and_then(|o| String::from_utf8(o.stdout).ok())
            .and_then(|s| s.trim().parse::<u64>().ok())
            .map(|b| b / 1024 / 1024)
            .unwrap_or(0)
    }
}

#[cfg(not(any(windows, target_os = "linux", target_os = "macos")))]
mod imp {
    pub fn total_memory_mb() -> u64 {
        0
    }
}
