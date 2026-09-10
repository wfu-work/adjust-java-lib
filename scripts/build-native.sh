#!/usr/bin/env bash
# 在 Go 库目录外生成当前目标平台的 C 共享库，不覆盖 SDK 内置资源。
set -euo pipefail

if [[ $# -ne 1 ]]; then
    echo "Usage: $0 /path/to/nav-adjust-go-lib" >&2
    exit 2
fi

sdk_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
go_source="$(cd -- "$1" && pwd)"
if [[ ! -f "$go_source/libs/adjust_api.h" ]]; then
    echo "Not a nav-adjust-go-lib source directory: $go_source" >&2
    exit 2
fi

native_os="$(go env GOOS)"
native_arch="$(go env GOARCH)"
case "$native_os" in
    darwin) extension=dylib ;;
    linux) extension=so ;;
    windows) extension=dll ;;
    *) echo "Unsupported target OS: $native_os" >&2; exit 2 ;;
esac
native_output="$sdk_root/target/native/$native_os-$native_arch"
mkdir -p "$native_output"
(cd "$go_source" && CGO_ENABLED=1 go build -buildmode=c-shared -o "$native_output/libAdjust.$extension" .)
cp "$go_source/libs/adjust_api.h" "$native_output/adjust_api.h"
printf 'Built native library: %s\n' "$native_output/libAdjust.$extension"
