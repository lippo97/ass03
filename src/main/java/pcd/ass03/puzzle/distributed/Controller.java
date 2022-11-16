package pcd.ass03.puzzle.distributed;

import io.vertx.core.AbstractVerticle;
import pcd.ass03.puzzle.concentrated.Tile;
import pcd.ass03.puzzle.distributed.commands.Click;
import pcd.ass03.puzzle.distributed.commands.Command;
import pcd.ass03.puzzle.distributed.commands.Initialize;
import pcd.ass03.puzzle.distributed.viewmodel.ViewModel;
import pcd.ass03.raft.RaftPeer;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Controller extends AbstractVerticle {
    private final RaftPeer<Command> raftPeer;
    private final ViewModel viewModel;
    private final AppParameters parameters;
    private final int id;
    private final State state;

    public Controller(RaftPeer<Command> raftPeer, ViewModel viewModel, AppParameters parameters) {
        this.raftPeer = raftPeer;
        this.viewModel = viewModel;
        this.parameters = parameters;
        this.id = parameters.raftParameters.getId();
        this.state = new State();
    }

    @Override
    public void start() throws Exception {
        super.start();

        viewModel.onTileClick((Tile tile) -> {
            vertx.runOnContext((v) -> {
                raftPeer.pushLogEntry(new Click(id, tile.getCurrentPosition()));
            });
        });

        raftPeer.onApplyLogEntry((entry) -> {
            System.out.println(id + " | applying " + entry);
            if (entry instanceof Initialize) {
                final var initialize = (Initialize) entry;
                state.initialized = true;
                viewModel.createTiles(initialize.positions);
                viewModel.setVisible(true);
            } else if (entry instanceof Click) {
                final var click = (Click) entry;
                viewModel.handleClick(click);
            }
        });
        raftPeer.onBecomeLeader((_id) -> {
            System.out.println(_id + " | I'm the leader " );
            if (!state.initialized) {
                final var positions = IntStream.range(0, parameters.rows * parameters.columns)
                    .boxed()
                    .collect(Collectors.toList());
                while (isListSorted(positions)) {
                    Collections.shuffle(positions);
                }
                raftPeer.pushLogEntry(new Initialize(positions));
                state.initialized = true;
            }
        });

        getVertx()
            .deployVerticle(raftPeer)
            .onSuccess((s) -> {
                System.out.println(id + " | Successfully deployed raft peer id=" + id);
                System.out.println(id + " | Waiting for the election...");
            });
    }

    private <A extends Comparable<A>> boolean isListSorted(List<A> list) {
        if (list.size() == 2) {
            return list.get(0).compareTo(list.get(1)) <= 0;
        } else if (list.size() == 1 || list.size() == 0) {
            return true;
        } else {
            return list.get(0).compareTo(list.get(1)) <= 0 &&
                isListSorted(list.subList(1, list.size()));
        }
    }

    static class State {
        public boolean initialized = false;
    }

}
