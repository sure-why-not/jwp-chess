package chess.serviece;

import chess.dto.ChessResponse;
import chess.dto.GameCreationRequest;
import chess.dao.GameDao;
import chess.dao.PieceDao;
import chess.dto.GameDto;
import chess.domain.ChessGame;
import chess.domain.GameStatus;
import chess.domain.Score;
import chess.domain.command.MoveCommand;
import chess.domain.piece.Piece;
import chess.domain.piece.PieceColor;
import chess.domain.position.Position;
import chess.dto.PieceDto;
import chess.dto.ScoresDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChessGameService {

    private final GameDao gameDao;
    private final PieceDao pieceDao;

    public ChessGameService(GameDao gameDao, PieceDao pieceDao) {
        this.gameDao = gameDao;
        this.pieceDao = pieceDao;
    }

    public Long addGame(GameCreationRequest gameCreationRequest) {
        ChessGame chessGame = ChessGame.initGame();
        long id = gameDao.save(createGameDto(chessGame, gameCreationRequest));
        List<PieceDto> pieceDtos = convertPieceDtos(chessGame.getPieces(), id);
        pieceDao.saveAll(pieceDtos);
        return id;
    }

    private GameDto createGameDto(ChessGame chessGame, GameCreationRequest gameCreationRequest) {
        PieceColor turnColor = chessGame.getTurnColor();
        if (chessGame.isRunning()) {
            return new GameDto(gameCreationRequest.getTitle(), gameCreationRequest.getPassword(), turnColor.getName(), "playing");
        }
        return new GameDto(gameCreationRequest.getTitle(), gameCreationRequest.getPassword(), turnColor.getName(), "finished");
    }

    private List<PieceDto> convertPieceDtos(Map<Position, Piece> pieces, long gameId) {
        return pieces.entrySet()
                .stream()
                .map(entry -> PieceDto.from(entry.getKey(), entry.getValue(), gameId))
                .collect(Collectors.toList());
    }

    public GameDto getGame(Long id, GameDto gameDto) {
        String password = gameDao.findPasswordById(id);
        if (!password.equals(gameDto.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        return gameDao.findGameById(id);
    }

    public List<GameDto> getAllGames() {
        return gameDao.findAll();
    }

    public void deleteGame(Long id, GameDto gameDto) {
        String password = gameDao.findPasswordById(id);
        if (!password.equals(gameDto.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        GameStatus gameStatus = gameDao.findStatusById(id);
        if (gameStatus == GameStatus.PLAYING) {
            throw new IllegalArgumentException("게임이 진행중입니다. 삭제할 수 없습니다.");
        }
        gameDao.removeById(id);
    }

    public ChessResponse getChessGame(Long gameId) {
        List<PieceDto> pieceDtos = pieceDao.findPiecesByGameId(gameId);
        GameDto gameDto = gameDao.findGameById(gameId);
        return new ChessResponse(gameId, pieceDtos, gameDto.getTurn(), gameDto.getStatus());
    }

    public ChessResponse movePiece(Long gameId, MoveCommand moveCommand) {
        ChessGame game = createGame(gameId);
        game.proceedWith(moveCommand);
        pieceDao.removeByPosition(gameId, moveCommand.to());
        pieceDao.updatePosition(gameId, moveCommand.from(), moveCommand.to());
        GameStatus gameStatus = GameStatus.FINISHED;
        if (game.isRunning()) {
            gameStatus = GameStatus.PLAYING;
        }
        gameDao.updateGame(gameId, game.getTurnColor().getName(), gameStatus.getName());
        return getChessGame(gameId);
    }

    private ChessGame createGame(Long gameId) {
        List<PieceDto> pieceDtos = pieceDao.findPiecesByGameId(gameId);
        GameDto gameDto = gameDao.findGameById(gameId);
        Map<Position, Piece> pieces = pieceDtos.stream()
                .map(PieceDto::toPieceEntry)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return new ChessGame(pieces, PieceColor.find(gameDto.getTurn()));
    }

    public ScoresDto getScore(Long gameId) {
        ChessGame game = createGame(gameId);
        Map<PieceColor, Score> scoresByColor = game.calculateScoreByColor();
        return ScoresDto.of(scoresByColor);
    }

    public ScoresDto finishGame(Long gameId) {
        gameDao.updateStatus(gameId, GameStatus.FINISHED);
        return getScore(gameId);
    }
}
