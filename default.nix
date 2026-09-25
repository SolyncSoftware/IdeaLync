{
  stdenv,
  gradle_9,
  makeWrapper,
}: let
  self = stdenv.mkDerivation (_finalAttrs: {
    pname = "IdeaLync";
    version = "1.0";

    src = ./.;
    nativeBuildInputs = [gradle_9];

    mitmCache = gradle_9.fetchDeps {
      pkg = self;
      # update or regenerate this by running
      #  $(nix build .#idealync.mitmCache.updateScript --print-out-paths)
      data = ./deps.json;
    };

    gradleBuildTask = "distTar";

    doCheck = true;

    installPhase = ''
      mkdir -p $out/
      tar xf build/distributions/IdeaLync.tar --strip-components=1 -C $out/
    '';
  });
in
  self
