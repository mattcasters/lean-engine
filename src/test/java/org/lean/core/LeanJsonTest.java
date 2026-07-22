package org.lean.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.lean.presentation.LeanPresentation;
import org.lean.presentation.theme.LeanTheme;

class LeanJsonTest {

  @Test
  void roundTripsPresentationWithoutHopRuntimeFields() throws Exception {
    LeanPresentation presentation = new LeanPresentation();
    presentation.setName("demo");
    presentation.setDescription("json round trip");
    presentation.getThemes().add(LeanTheme.getDefault());
    presentation.setDefaultThemeName(Constants.DEFAULT_THEME_NAME);

    ObjectMapper mapper = LeanJson.createMapper();
    String json = mapper.writeValueAsString(presentation);
    assertFalse(json.contains("\"fullName\""), "fullName should be ignored in JSON");
    assertFalse(json.contains("metadataProviderName"));

    LeanPresentation restored = mapper.readValue(json, LeanPresentation.class);
    assertNotNull(restored);
    assertEquals("demo", restored.getName());
    assertTrue(restored.getThemes().size() >= 1);
  }
}
