package org.springframework.core.env.scan2;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static org.springframework.core.env.EnvironmentSystemIntegrationTests.Constants.*;

@Profile(DEV_ENV_NAME)
@Component(DEV_BEAN_NAME)
class DevBean { }

@Profile(PROD_ENV_NAME)
@Component(PROD_BEAN_NAME)
class ProdBean { }
