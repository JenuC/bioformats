BIOFORMATS_JAR = $(shell find ../components -name "bioformats_package*.jar" | head -n 1)
ifeq ($(BIOFORMATS_JAR),)
	BIOFORMATS_JAR = $(shell find ../components/bundles/bioformats_package/target -name "bioformats_package*.jar" | grep -v "sources" | grep -v "javadoc" | head -n 1)
endif

all: build

build:
	@echo "Building SDTReaderDebug..."
	@mkdir -p build
	javac -cp $(BIOFORMATS_JAR) -d build src/SDTReaderDebug.java
	@echo "Build complete."

run:
	@if [ -z "$(FILE)" ]; then \
		echo "Usage: make run FILE=/path/to/your/file.sdt"; \
		exit 1; \
	fi
	@echo "Running SDTReaderDebug on $(FILE)..."
	java -cp build:$(BIOFORMATS_JAR) SDTReaderDebug "$(FILE)"

clean:
	@echo "Cleaning up..."
	rm -rf build
	@echo "Cleanup complete."

.PHONY: all build run clean 