#!/usr/bin/env bash
# Muestra las firmas (javap) de clases de Minecraft con los nombres de Mojang, para la versión del proyecto.
# Uso: ./scripts/api-probe/probe.sh [fichero-con-clases]   (requiere haber ejecutado ./gradlew build antes)
set -euo pipefail

CLASSES_FILE="${1:-scripts/api-probe/classes.txt}"

jar=""
while IFS= read -r candidate; do
	if unzip -l "$candidate" 2>/dev/null | grep -q 'net/minecraft/client/Minecraft.class'; then
		jar="$candidate"
		break
	fi
done < <(find "$HOME/.gradle" .gradle build -name '*.jar' -path '*minecraft*' 2>/dev/null | grep -v -- '-sources' | sort)

if [ -z "$jar" ]; then
	echo "No se encontró el jar de Minecraft mapeado" >&2
	exit 1
fi
echo "Jar: $jar"

classpath="$jar"
while IFS= read -r lib; do classpath="$classpath:$lib"; done < <(find "$HOME/.gradle/caches/modules-2" -name '*.jar' 2>/dev/null)

grep -v '^\s*\(#\|$\)' "$CLASSES_FILE" | while IFS= read -r class; do
	echo
	echo "==================== $class"
	javap -p -cp "$classpath" "$class" 2>&1 | grep -v -E 'lambda\$|access\$|\$\$' || true
done
