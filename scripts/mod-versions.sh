#!/usr/bin/env bash
# Lista las últimas versiones en Modrinth de los mods integrados para la versión de Minecraft del proyecto.
# Las mods que no existen en Modrinth o no tienen versión para esta versión de Minecraft aparecen vacías.
set -uo pipefail

MC_VERSION=$(grep '^minecraft_version=' gradle.properties | cut -d= -f2)
MODS="${MODS:-sodium lithium ferrite-core immediatelyfast dynamic-fps continuity cull-less-leaves debugify mouse-tweaks not-enough-crashes crashpatch lambdabettergrass enhanced-block-entities moreculling entityculling very-many-players c2me-fabric servercore fast-ip-ping optigui cit-resewn wavey-capes shulkerboxtooltip scalablelux noisium}"

for mod in $MODS; do
	echo "== $mod (Minecraft $MC_VERSION)"
	curl -fsS -G "https://api.modrinth.com/v2/project/$mod/version" \
		--data-urlencode "game_versions=[\"$MC_VERSION\"]" \
		--data-urlencode 'loaders=["fabric"]' \
		-H 'User-Agent: FreedomClient/mod-versions' 2>/dev/null |
		jq -r '.[:3][] | "  \(.version_number)  id=\(.id)  [\(.version_type), \(.date_published[:10])]  deps: \([.dependencies[] | select(.dependency_type == "required") | .project_id] | join(","))"' || echo "  (not found)"
done
