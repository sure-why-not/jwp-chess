package chess.dao;


import chess.dto.GameDto;
import chess.dto.GameStatusDto;

public class FakeGameDao implements GameDao {

    private GameDto gameDto;

    @Override
    public void removeAll() {
        gameDto = null;
    }

    @Override
    public void saveGame(GameDto gameDto) {
        this.gameDto = gameDto;
    }

    @Override
    public void updateGame(GameDto gameDto) {
        this.gameDto = gameDto;
    }

    @Override
    public void updateStatus(GameStatusDto statusDto) {
        this.gameDto = new GameDto(this.gameDto.getTurn(), statusDto.getName());
    }

    @Override
    public GameDto findGame() {
        return gameDto;
    }
}
