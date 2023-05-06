import weaver.interfaces.schedule.BaseCronJob;

/**
 * @author Li Yu Feng
 * @date 2023-03-13 14:57
 */
public class Test04 extends BaseCronJob {
    String name;

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void execute() {

    }
}
