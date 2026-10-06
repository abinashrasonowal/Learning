package command.solution;

import java.util.List;

public class MacroCommand implements Command {
    private final String name;
    private final List<Command> steps;

    public MacroCommand(String name, List<Command> steps) {
        this.name = name;
        this.steps = List.copyOf(steps);
    }

    @Override
    public void execute() {
        for (Command step : steps) {
            step.execute();
        }
    }

    @Override
    public void undo() {
        // Reverse order: each step's undo assumes the state right after that step ran.
        for (int i = steps.size() - 1; i >= 0; i--) {
            steps.get(i).undo();
        }
    }

    @Override
    public String toString() {
        return "Macro(" + name + ")";
    }
}
