package core;

import java.io.File;
import java.io.IOException;

public interface EvidenceWriter {

    File write(EvidenceModel model, File evidenceFolder, String outputBaseName) throws IOException;

    default File write(EvidenceModel model, File evidenceFolder) throws IOException {
        return write(model, evidenceFolder, model.getFileName());
    }

    static EvidenceWriter forModel(EvidenceModel model) {
        return model.getFormat() == EvidenceModel.Format.WORD
                ? new WordEvidenceWriter()
                : new ExcelEvidenceWriter();
    }
}