"""Package an existing Maven executable JAR for Elastic Beanstalk Java SE."""
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED

root = Path(__file__).resolve().parents[2]
jars = list((root / "target").glob("*.jar"))
if len(jars) != 1:
    raise SystemExit("Expected one executable JAR in target; run mvn clean verify first.")

output = root / "target" / "aws-backend.zip"
with ZipFile(output, "w", ZIP_DEFLATED) as bundle:
    bundle.write(jars[0], "application.jar")
    bundle.write(Path(__file__).with_name("Procfile"), "Procfile")
    bundle.write(Path(__file__).with_name("schema.sql"), "deploy-schema.sql")
    platform = Path(__file__).parent / ".platform"
    for asset in platform.rglob("*"):
        if asset.is_file():
            bundle.write(asset, asset.relative_to(Path(__file__).parent))
print(output)
