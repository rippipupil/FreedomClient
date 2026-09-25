#!/usr/bin/env bash
# Lista las últimas versiones en Modrinth de los mods integrados para la versión de Minecraft del proyecto.
set -euo pipefail

MC_VERSION=$(grep '^minecraft_version=' gradle.properties | cut -d= -f2)
MODS="${MODS:-sodium lithium ferrite-core immediatelyfast scalablelux dynamic-fps}"

for mod in $MODS; do
	echo "== $mod (Minecraft $MC_VERSION)"
	curl -fsS -G "https://api.modrinth.com/v2/project/$mod/version" \
		--data-urlencode "game_versions=[\"$MC_VERSION\"]" \
		--data-urlencode 'loaders=["fabric"]' \
		-H 'User-Agent: FreedomClient/mod-versions' |
		jq -r '.[:3][] | "  \(.version_number)  [\(.version_type), \(.date_published[:10])]  deps: \([.dependencies[] | select(.dependency_type == "required") | .project_id] | join(","))"'
done
