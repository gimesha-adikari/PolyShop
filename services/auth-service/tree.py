from __future__ import annotations
import argparse
import os
from pathlib import Path
import sys
import re
import fnmatch

DEFAULT_EXCLUDED_DIRS = {
    ".git", ".hg", ".svn", ".idea", ".vs", ".vscode",
    "node_modules", ".next", ".nuxt", ".cache", ".parcel-cache", ".vite",
    ".svelte-kit", ".angular", ".yarn", ".pnp", ".pnpm-store", ".vercel",
    ".netlify", ".docusaurus",
    "__pycache__", ".pytest_cache", ".mypy_cache", ".ruff_cache", ".tox",
    ".ipynb_checkpoints", ".venv", "venv", "env", ".eggs",
    ".gradle", ".build", "build", "out", "libs",
    "Pods", "DerivedData",
    "target",
    "bin",
    "obj",
    ".terraform", ".serverless",
    ".firebase", ".expo", ".dart_tool",
    "collect_text_and_tree.py",
}

DEFAULT_EXCLUDED_DIR_PATTERNS = [
    r".*\.egg-info$",
]

DEFAULT_EXCLUDED_FILES = {
    "package-lock.json", "yarn.lock", "pnpm-lock.yaml", "bun.lockb",
    "Pipfile.lock", "poetry.lock", "requirements.txt.lock",
    "composer.lock", "Gemfile.lock", "Cargo.lock",
    "Podfile.lock",
    "gradle-wrapper.jar", ".DS_Store", "Thumbs.db",
    ".coverage",
}

DEFAULT_EXCLUDED_FILE_GLOBS = [
    ".coverage.*",
    "*.pyc", "*.pyo", "*.pyd",
    "*.class",
    "*.o", "*.obj", "*.a", "*.so", "*.dll", "*.dylib",
    "*.map",
    "*.local.env",
    "*.txt",
]

def _compile_dir_regexes(patterns: list[str]) -> list[re.Pattern]:
    return [re.compile(p) for p in patterns]

DEFAULT_EXCLUDED_DIR_REGEXES = _compile_dir_regexes(DEFAULT_EXCLUDED_DIR_PATTERNS)

def dir_is_excluded(name: str, name_set: set[str], regexes: list[re.Pattern], extra_globs: list[str]) -> bool:
    if name in name_set:
        return True
    for rgx in regexes:
        if rgx.match(name):
            return True
    for g in extra_globs:
        if fnmatch.fnmatch(name, g):
            return True
    return False

def file_is_excluded(name: str, name_set: set[str], globs: list[str]) -> bool:
    if name in name_set:
        return True
    for g in globs:
        if fnmatch.fnmatch(name, g):
            return True
    return False

def generate_tree(root: Path, exclude_dirs, exclude_dir_regex, exclude_dir_globs,
                  exclude_files, exclude_file_globs) -> list[str]:

    lines = [f"{root.name}/"]

    def walk(path: Path, prefix: str):
        try:
            items = sorted(path.iterdir(), key=lambda p: (not p.is_dir(), p.name.lower()))
        except PermissionError:
            return

        filtered = []
        for e in items:
            name = e.name
            if e.is_dir():
                if dir_is_excluded(name, exclude_dirs, exclude_dir_regex, exclude_dir_globs):
                    continue
            else:
                if file_is_excluded(name, exclude_files, exclude_file_globs):
                    continue
            filtered.append(e)

        for i, e in enumerate(filtered):
            last = (i == len(filtered) - 1)
            connector = "└── " if last else "├── "
            if e.is_dir():
                lines.append(prefix + connector + e.name + "/")
                walk(e, prefix + ("    " if last else "│   "))
            else:
                lines.append(prefix + connector + e.name)

    walk(root, "")
    return lines

def main():
    ap = argparse.ArgumentParser(description="Write project tree only (folders + filenames).")
    ap.add_argument("root", type=Path, help="Root directory")
    ap.add_argument("output", type=Path, help="Output text file")
    args = ap.parse_args()

    root = args.root.resolve()

    lines = generate_tree(
        root,
        DEFAULT_EXCLUDED_DIRS,
        DEFAULT_EXCLUDED_DIR_REGEXES,
        [],
        DEFAULT_EXCLUDED_FILES,
        DEFAULT_EXCLUDED_FILE_GLOBS
    )

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text("\n".join(lines), encoding="utf-8")
    print(f"Tree written to: {args.output}")

if __name__ == "__main__":
    main()
