package io.github.reionchan.demo.observation;

import io.github.reionchan.demo.observation.context.DemoContext;
import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.docs.ObservationDocumentation;

/**
 * 演示类的观测记录枚举类
 *
 * @author Reion
 * @date 2023-11-22
 **/
public enum DemoObservationDocument implements ObservationDocumentation {

    DEFAULT {
        /**
         * 绑定演示类的观测约定类
         */
        @Override
        public Class<? extends ObservationConvention<? extends Observation.Context>> getDefaultConvention() {
            return DefaultDemoObservationConvention.class;
        }

        @Override
        public KeyName[] getLowCardinalityKeyNames() {
            return DemoObservationDocument.DemoLowCardinalityKeyNames.values();
        }

        /**
         * 设置 {@link DemoContext#getName()} 名称
         */
        @Override
        public String getName() {
            return "demo";
        }

        /**
         * 设置对人友好的 {@link DemoContext#getName()} 名称
         */
        @Override
        public String getContextualName() {
            return "human.friendly.name.demo";
        }
    };

    private DemoObservationDocument() {
    }

    static enum DemoLowCardinalityKeyNames implements KeyName {

        /**
         * 调用方法名称
         */
        CALL_METHOD {
            @Override
            public String asString() {
                return "demo.call.method";
            }
        },

        /**
         * 备注，选填项
         */
        NOTE {
            @Override
            public String asString() {
                return "demo.note";
            }

            @Override
            public boolean isRequired() {
                return false;
            }
        };

        private DemoLowCardinalityKeyNames() {
        }
    }
}
