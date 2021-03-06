package com.cookierdelivery.cdk;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import software.amazon.awscdk.core.App;

public class CdkTest {
  private static final ObjectMapper JSON =
      new ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true);

  @Test
  public void testStack() throws IOException {
    App app = new App();
    CdkStack stack = new CdkStack(app, "test");

    // synthesize the stack to a CloudFormation template
    JsonNode actual =
        JSON.valueToTree(app.synth().getStackArtifact(stack.getArtifactId()).getTemplate());

    // Update once resources have been added to the stack
    assertThat(actual.get("Resources")).isNull();
  }
}
