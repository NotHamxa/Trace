package com.logiclab.model;

public abstract class OutputComponent extends Component {
    public OutputComponent(String name, double width, double height) {
        super(name, width, height);
    }

    public abstract boolean isActive();
}
