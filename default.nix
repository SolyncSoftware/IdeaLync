{ bun2nix, ... }:

bun2nix.mkDerivation {
  pname = "idealync";
  version = "1.0.0";
  src = ./.;

  bunDeps = bun2nix.fetchBunDeps {
    bunNix = ./bun.nix;
  };

  module = "src/index.ts";

  # hacky way to let bun writee to the cache dir
  postBunSetInstallCacheDirPhase = ''
    chmod -R u+w "$BUN_INSTALL_CACHE_DIR"
  '';
}
