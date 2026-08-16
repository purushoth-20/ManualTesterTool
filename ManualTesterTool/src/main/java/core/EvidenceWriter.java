package core;

import java.io.File;
import java.io.IOException;

public interface EvidenceWriter {

    /** Regenerates the full evidence file from the current model state. */
    File write(EvidenceModel model, File evidenceFolder) throws IOException;

    static EvidenceWriter forModel(EvidenceModel model) {
        return model.getFormat() == EvidenceModel.Format.WORD
                ? new WordEvidenceWriter()
                : new ExcelEvidenceWriter();
    }
}
