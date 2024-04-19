import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;

import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;

//represents an edge
class Edge {
  GamePiece from;
  GamePiece to;
  int weight;

  Edge(GamePiece from, GamePiece to, int weight) {
    this.from = from;
    this.to = to;
    this.weight = weight;
  }
}

//comparator to compare edges by weight 
class CompareByWeight implements Comparator<Edge> {
  public int compare(Edge edge1, Edge edge2) {
    return edge1.weight - edge2.weight;
  }
}

//class for storing Edge sets
class UnionFind<T> {
  HashMap<T, T> representatives;
  //.put, .get, .containsKey

  UnionFind() {
    this.representatives = new HashMap<T, T>();
  }

  // Effect: Union two sets containing elements x and y
  public void union(T x, T y) {
    T rootX = find(x);
    T rootY = find(y);
    if (!rootX.equals(rootY)) {
      representatives.put(rootX, rootY);
    }
  }

  // Find the root of the set containing element x
  public T find(T x) {
    if (!representatives.containsKey(x)) {
      representatives.put(x, x);
      return x;
    }
    if (!representatives.get(x).equals(x)) {
      representatives.put(x, find(representatives.get(x)));
    }
    return representatives.get(x);
  }


  // Check if x and y have the same root
  public boolean isConnected(T x, T y) {
    return find(x).equals(find(y));
  }
}


//represents the LightEmAll game 
class LightEmAll extends World {
  // a list of columns of GamePieces,
  // i.e., represents the board in column-major order
  ArrayList<ArrayList<GamePiece>> board;
  // a list of all nodes
  ArrayList<GamePiece> nodes;
  // a list of edges of the minimum spanning tree
  ArrayList<Edge> mst;
  // the width and height of the board
  int width;
  int height;
  int size; 
  // the current location of the power station,
  // as well as its effective radius
  int powerRow = 0;
  int powerCol = 0;
  int radius;
  Random r;
  int time = 0;
  int numClicks = 0;

  boolean gameOver;
  ArrayList<String> directions = new ArrayList<String>();

  //constructor using kruskal's 
  LightEmAll(int width, int height, boolean kruskal) {
    this.width = width;
    this.height = height;
    this.board = this.makeBoard();
    this.size = 50;
    this.gameOver = false;
    this.r = new Random();
    directions.add("left");
    directions.add("right");
    directions.add("top");
    directions.add("bottom");

    if (kruskal) {
      generateNodes();
      this.mst = kruskalMST();
      connectEdges();
      this.randomize(r);
    }
    else {
      this.board = this.makeBoard1();
      this.randomize(r);
    }

    this.bfs();
  }

  //constructor using kruskal's for testing  
  LightEmAll(int width, int height, Random r, boolean kruskal) {
    this.width = width;
    this.height = height;
    this.board = this.makeBoard();
    this.size = 50;
    this.gameOver = false;
    this.r = r;
    directions.add("left");
    directions.add("right");
    directions.add("top");
    directions.add("bottom");

    if (kruskal) {
      generateNodes();
      this.mst = kruskalMST();
      connectEdges();
      this.randomize(r);
    }
    else {
      this.board = this.makeBoard1();
      this.randomize(r);
    }

    this.bfs();
  }

  //original constructor 
  LightEmAll(int width, int height) {
    this.width = width;
    this.height = height;
    this.board = this.makeBoard();
    this.size = 50;
    this.gameOver = false;
    r = new Random();
    this.randomize(r);
    directions.add("left");
    directions.add("right");
    directions.add("top");
    directions.add("bottom");
    this.mst = new ArrayList<Edge>();
    this.bfs();
  }


  //places the board on top of a worldscene
  public WorldScene makeScene() {

    WorldScene scene = new WorldScene(this.height * this.size, this.width * this.size);
    scene.placeImageXY(this.drawBoard(this.size), 
        this.width * this.size / 2, this.height * this.size / 2);

    return scene;
  }

  // draws the game board
  public WorldImage drawBoard(int size) {

    WorldImage boardImage = new EmptyImage();
    for (ArrayList<GamePiece> col : this.board) {
      boardImage = new BesideImage(boardImage, this.drawCol(col, size));
    }
    return boardImage;
  }

  // Effect: adds 1 second to every clock tick
  public void onTick() {
    time++;
  }

  //draws a col
  public WorldImage drawCol(ArrayList<GamePiece> col, int size) {
    WorldImage colImage = new EmptyImage();

    for (GamePiece piece : col) {
      WorldImage regTile = piece.tileImage(size, 10, Color.LIGHT_GRAY, false);
      WorldImage poweredTile = piece.tileImage(size, 10, Color.yellow, false);
      WorldImage powerStationTile = piece.tileImage(size, 10, Color.yellow, true);

      if (piece.powerStation) {
        colImage = new AboveImage(colImage, powerStationTile);
      }
      else if (piece.isPowered()) {
        colImage = new AboveImage(colImage, poweredTile);
      }
      else {
        colImage = new AboveImage(colImage, regTile);
      }
    }
    return colImage;
  }

  // initial make board from part 1 
  public ArrayList<ArrayList<GamePiece>> makeBoard1() {
    ArrayList<ArrayList<GamePiece>> board = new ArrayList<ArrayList<GamePiece>>();

    for (int i = 0; i < this.width; i++) {
      ArrayList<GamePiece> row = new ArrayList<GamePiece>();
      for (int j = 0; j < this.height; j++) {
        // if on the first column 
        // boolean left, boolean right, boolean top, boolean bottom
        if (i == 0) {
          row.add(new GamePiece(i, j, false, true, false, false, false));  
        }
        // if on the last column 
        else if (i == this.width - 1) {
          row.add(new GamePiece(i, j, true, false, false, false, false));
        }
        // if in the middle and top
        else if (i == this.width / 2 && j == 0) {
          row.add(new GamePiece(i, j, true, true, false, true, false));
        }
        // if in the middle and bottom
        else if (i == this.width / 2 && j == height - 1) {
          row.add(new GamePiece(i, j, true, true, true, false, false));
        }
        // if in the middle
        else if (i == this.width / 2 && j != this.height / 2) {
          row.add(new GamePiece(i, j, true, true, true, true, false));
        }
        // power station
        else if (i == this.width / 2 && j == this.height / 2) {
          row.add(new GamePiece(i, j, true, true, true, true, true));
          this.powerRow = j;
          this.powerCol = i;
        }
        // else only left and right
        else {
          row.add(new GamePiece(i, j, true, true, false, false, false));
        }
      }
      board.add(row);
    }
    return board;
  }

  // constructs the board
  public ArrayList<ArrayList<GamePiece>> makeBoard() {
    ArrayList<ArrayList<GamePiece>> board = new ArrayList<ArrayList<GamePiece>>();

    for (int i = 0; i < this.width; i++) {
      ArrayList<GamePiece> row = new ArrayList<GamePiece>();
      for (int j = 0; j < this.height; j++) {
        GamePiece gp = new GamePiece(i, j);
        // power station at top left corner 
        if (i == 0 && j == 0) {
          row.add(new GamePiece(i, j, false, false, false, false, true));
        }

        else {
          new GamePiece(i, j).randGP(new Random());
          row.add(gp);
        }
      }
      board.add(row);
    }

    return board;
  }

  // Effect: determines whether all the game pieces on the board have been powered up
  public void isGameOver() {
    int pieces = this.width * this.height;
    int count = 0;
    for (int i = 0; i < this.width; i++) {
      for (int j = 0; j < this.height; j++) {
        if (this.board.get(i).get(j).isPowered()) {
          count += 1;
        }
      }
    }
    gameOver = (pieces == count);
  }

  // Effect: conducts the binary search through the game to power the cells
  void bfs() {
    GamePiece to;
    GamePiece from;
    from = this.board.get(powerCol).get(powerRow);
    ArrayList<GamePiece> worklist = new ArrayList<GamePiece>();
    worklist.add(from);
    for (ArrayList<GamePiece> gp : this.board) {
      for (GamePiece g: gp) {
        g.powered = false;
      }
    }
    from.powered = true;
    while (!worklist.isEmpty()) {
      from = worklist.remove(0);
      for (String dir: this.directions) {
        to = searchHelp(from.col, from.row, dir);
        if (to != null && !to.powered) {
          to.powered = true;
          worklist.add(to);
        }
      }
    }
    this.isGameOver();
  }

  //helper for checking neighbors 
  GamePiece searchHelp(int col, int row, String currentDirection) {
    GamePiece currentPiece = this.board.get(col).get(row);

    // Check if the current piece exists and has the desired direction
    if (currentPiece == null) {
      return null;
    }

    // Determine the coordinates of the neighboring piece based on the direction
    int neighborCol = col;
    int neighborRow = row;

    if (currentDirection.equals("left")) {
      neighborCol -= 1;
    } else if (currentDirection.equals("right")) {
      neighborCol += 1;
    } else if (currentDirection.equals("top")) {
      neighborRow -= 1;
    } else if (currentDirection.equals("bottom")) {
      neighborRow += 1;
    }

    // Check if the neighboring piece is within the bounds of the board
    if (neighborCol < 0 || neighborCol >= this.width || neighborRow < 0 
        || neighborRow >= this.height) {
      return null;
    }

    GamePiece neighborPiece = this.board.get(neighborCol).get(neighborRow);

    // Check if the neighboring piece exists
    if (neighborPiece == null) {
      return null;
    }

    // Check if the neighboring piece has the corresponding connection to the current piece
    if (currentDirection.equals("left") && currentPiece.hasLeft() 
        && neighborPiece.hasRight()) {
      return neighborPiece;
    } else if (currentDirection.equals("right") && currentPiece.hasRight() 
        && neighborPiece.hasLeft()) {
      return neighborPiece;
    } else if (currentDirection.equals("top") && currentPiece.hasTop() 
        && neighborPiece.hasBot()) {
      return neighborPiece;
    } else if (currentDirection.equals("bottom") && currentPiece.hasBot() 
        && neighborPiece.hasTop()) {
      return neighborPiece;
    } else {
      return null;
    }
  }

  // Effect: uses arrow keys to move the power station
  public void onKeyEvent(String key) {
    if (key.equals("up") && this.powerRow != 0) {
      if (this.board.get(this.powerCol).get(this.powerRow).hasTop()
          && this.board.get(this.powerCol).get(this.powerRow - 1).hasBot()) {
        // this becomes not power station
        this.board.get(this.powerCol).get(this.powerRow).movePower();
        this.board.get(this.powerCol).get(this.powerRow - 1).movePower();
        // update power station row
        this.powerRow -= 1;
      }
    }
    if (key.equals("down") && this.powerRow != this.height - 1) {
      if (this.board.get(powerCol).get(powerRow).hasBot() 
          && this.board.get(this.powerCol).get(this.powerRow + 1).hasTop()) {
        // this becomes not power station
        this.board.get(this.powerCol).get(this.powerRow).movePower();
        this.board.get(this.powerCol).get(this.powerRow + 1).movePower();
        // update power station row
        this.powerRow += 1;
      }
    }
    if (key.equals("left") && powerCol != 0) {
      if (this.board.get(powerCol).get(powerRow).hasLeft() 
          && this.board.get(this.powerCol - 1).get(this.powerRow).hasRight()) {
        // this becomes not power station
        this.board.get(this.powerCol).get(this.powerRow).movePower();
        this.board.get(this.powerCol - 1).get(this.powerRow).movePower();
        // update power station row
        this.powerCol -= 1;
      }
    }
    if (key.equals("right") && powerCol != width - 1) {
      if (this.board.get(powerCol).get(powerRow).hasRight() 
          && this.board.get(this.powerCol + 1).get(this.powerRow).hasLeft()) {
        // this becomes not power station
        this.board.get(this.powerCol).get(this.powerRow).movePower();
        this.board.get(this.powerCol + 1).get(this.powerRow).movePower();
        // update power station row
        this.powerCol += 1;
      }
    }
    this.bfs();
  }

  // Effect: rotates the game piece 
  public void onMouseClicked(Posn pos) {
    int tileSize = this.size;
    int clickedCol = pos.x / tileSize;
    int clickedRow = pos.y / tileSize;

    if (clickedCol >= 0 && clickedCol < width 
        && clickedRow >= 0 && clickedRow < height) {
      GamePiece clickedPiece  = board.get(clickedCol).get(clickedRow);
      numClicks++;
      clickedPiece.rotateClockwise(); 
      this.bfs();
    }   
    //if there is a connection, draw tile with wirecolor as yellow 
  }

  // Effect: randomizes every game piece by rotating random number of times 
  public void randomize(Random r) {
    for (int i = 0; i < this.width; i++) {
      for (int j = 0; j < this.height; j++) {
        for (int k = 0; k < r.nextInt(4); k++) {
          this.board.get(i).get(j).rotateClockwise();
        }
      }
    }
  }

  //Effect: generate all nodes in the game 
  public void generateNodes() {

    this.nodes = new ArrayList<>();

    for (int i = 0; i < width; i++) {
      for (int j = 0; j < height; j++) {

        this.nodes.add(board.get(i).get(j));
      }
    }
  }

  // find the mst using kruskal's 
  public ArrayList<Edge> kruskalMST() { 
    ArrayList<Edge> edgesInTree = new ArrayList<>();
    ArrayList<Edge> worklist = generateEdges();
    worklist.sort(new CompareByWeight());

    UnionFind<GamePiece> uf = new UnionFind<>();

    int edgeCount = 0;

    if (edgeCount < this.nodes.size()) {
      for (Edge edge : worklist) {
        if (!uf.isConnected(edge.from, edge.to)) {
          edgesInTree.add(edge);
          uf.union(edge.from, edge.to);
          edgeCount++;
        }

      }
    }
    return edgesInTree;
  }

  // generate edges of the game for mst 
  public ArrayList<Edge> generateEdges() {
    ArrayList<Edge> edges = new ArrayList<>();
    for (GamePiece vertex : this.nodes) {
      int col = vertex.col;
      int row = vertex.row;

      if (col > 0) {
        edges.add(new Edge(vertex, board.get(col - 1).get(row), r.nextInt(100)));
      }
      if (row > 0) {
        edges.add(new Edge(vertex, board.get(col).get(row - 1), r.nextInt(100)));
      }
    }
    return edges;
  }

  // Effect: connects the edges of the game 
  public void connectEdges() {
    for (Edge edge : mst) {
      GamePiece from = edge.from;
      GamePiece to = edge.to;

      if (from.col < to.col) {
        from.right = true;
        to.left = true;
      } else if (from.col > to.col) {
        from.left = true;
        to.right = true;
      } else if (from.row < to.row) {
        from.bottom = true;
        to.top = true;
      } else if (from.row > to.row) {
        from.top = true;
        to.bottom = true;
      }
    }
  }

  //ends the world 
  public WorldEnd worldEnds() {
    TextImage winText = new TextImage("YOU WIN", this.size, Color.green);
    String timeTakenStr = "Time Taken: " + Integer.toString(time) + " seconds";
    TextImage timeTaken = new TextImage(timeTakenStr, this.size, Color.red);
    String clicksTakenStr = "Number of Clicks: " + Integer.toString(numClicks);
    TextImage clicksTaken = new TextImage(clicksTakenStr, this.size, Color.red);
    WorldScene scene = this.makeScene();


    if (gameOver) {
      scene.placeImageXY(winText, (this.size * this.width) / 2, 
          (this.size * this.height) / 4);
      scene.placeImageXY(timeTaken, (this.size * this.width) / 2, 
          (this.size * this.height) / 2);
      scene.placeImageXY(clicksTaken, (this.size * this.width) / 2, 
          (this.size * this.height * 3) / 4);
      return new WorldEnd(true, scene);
    }
    else {
      return new WorldEnd(false, scene);
    }
  }

}


// represents a game piece in the lightEmAll game
class GamePiece {
  //in logical coordinates, with the origin
  // at the top-left corner of the screen
  int row;
  int col;
  // whether this GamePiece is connected to the
  // adjacent left, right, top, or bottom pieces
  boolean left;
  boolean right;
  boolean top;
  boolean bottom;
  // whether the power station is on this piece
  boolean powerStation;
  boolean powered;

  // gamePieces that can connect to this one
  //ArrayList<Edge> outEdges;

  GamePiece(int col,int row, boolean left, boolean right, boolean top, 
      boolean bottom,  boolean powerStation) {
    this.row = row;
    this.col = col;
    this.left = left;
    this.right = right;
    this.top = top; 
    this.bottom = bottom;
    this.powerStation = powerStation;
    if (this.powerStation) {
      this.powered = true;
    }
    else {
      this.powered = false;
    }

  }

  GamePiece(int col, int row) {
    this.row = row;
    this.col = col;
    this.left = false;
    this.right = false;
    this.top = false; 
    this.bottom = false;
    this.powerStation = false;
    this.powered = false;
  }


  //Generate an image of this, the given GamePiece.
  // - size: the size of the tile, in pixels
  // - wireWidth: the width of wires, in pixels
  // - wireColor: the Color to use for rendering wires on this
  // - hasPowerStation: if true, draws a fancy star on this tile to represent the power station
  WorldImage tileImage(int size, int wireWidth, Color wireColor, boolean hasPowerStation) {
    // Start tile image off as a blue square with a wire-width square in the middle,
    // to make image "cleaner" (will look strange if tile has no wire, but that can't be)
    WorldImage image = new OverlayImage(
        new RectangleImage(wireWidth, wireWidth, OutlineMode.SOLID, wireColor),
        new RectangleImage(size, size, OutlineMode.SOLID, Color.DARK_GRAY));
    WorldImage outline = new RectangleImage(size, size, OutlineMode.OUTLINE, Color.black);
    WorldImage vWire = new RectangleImage(wireWidth, (size + 1) / 2, OutlineMode.SOLID, wireColor);
    WorldImage hWire = new RectangleImage((size + 1) / 2, wireWidth, OutlineMode.SOLID, wireColor);

    if (this.top) { 
      image = new OverlayImage(outline, 
          new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP, vWire, 0, 0, image));
    }
    if (this.right) {
      image = new OverlayImage(outline, 
          new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE, hWire, 0, 0, image));
    }
    if (this.bottom) {
      image = new OverlayImage(outline,
          new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM, vWire, 0, 0, image));
    }
    if (this.left) {
      image = new OverlayImage(outline,
          new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE, hWire, 0, 0, image));
    }
    if (hasPowerStation) {
      image = new OverlayImage(outline, new OverlayImage(
          new OverlayImage(
              new StarImage(size / 3, 7, OutlineMode.OUTLINE, new Color(255, 128, 0)),
              new StarImage(size / 3, 7, OutlineMode.SOLID, new Color(0, 255, 255))),
          image));
    }
    return image;
  }

  // Effect: produces a random game piece 
  public void randGP(Random r) {
    this.top = r.nextBoolean();
    this.left = r.nextBoolean();
    this.bottom = r.nextBoolean();
    this.right = r.nextBoolean();
  }

  // Effect: moves the power station
  public void movePower() {
    this.powerStation = !this.powerStation;

    if (this.powerStation) {
      this.powered = true;
    }
    else {
      this.powered = false;
    }
  }

  // returns whether the game piece has a top
  public boolean hasTop() {
    return this.top;
  }

  // returns whether the game piece has a bottom
  public boolean hasBot() {
    return this.bottom;
  }

  // returns whether the game piece has a left
  public boolean hasLeft() {
    return this.left;
  }

  // returns whether the game piece has a right
  public boolean hasRight() {
    return this.right;
  }

  // returns whether the game piece is powered
  public boolean isPowered() {
    return this.powered;
  }

  // Effect: rotates this GamePiece clockwise 
  public void rotateClockwise() {
    boolean top = this.top; //original top
    this.top = this.left;
    this.left = this.bottom;
    this.bottom = this.right;
    this.right = top;
  }

}

class ExamplesLight {

  LightEmAll lem1;
  LightEmAll lem2;
  LightEmAll lem3;
  LightEmAll lem4;
  LightEmAll lem5;

  LightEmAll lem1Kruskal;
  LightEmAll lemKruskal;
  LightEmAll kruskalSmall;

  UnionFind<GamePiece> uf = new UnionFind<GamePiece>();

  GamePiece gp1;
  GamePiece gp2;
  GamePiece gp3;
  GamePiece gp4;
  GamePiece gp5;
  GamePiece gp6;
  GamePiece gp7;
  GamePiece gp8;
  GamePiece gp9;

  Edge edge1;
  Edge edge2;
  Edge edge3;
  Edge edge4;
  Edge edge5;
  Edge edge6;
  Edge edge7;
  Edge edge8;
  Edge edge9;
  Edge edge10;
  Edge edge11;
  Edge edge12;
  ArrayList<Edge> sorted;
  ArrayList<Edge> total;

  void initConditions() {

    this.lem1 = new LightEmAll(5, 4, new Random(1), false);
    this.lem2 = new LightEmAll(5, 4, new Random(1), false);
    this.lem3 = new LightEmAll(8, 9, new Random(1), false);
    this.lem4 = new LightEmAll(5, 4, new Random(1), false);
    this.lem5 = new LightEmAll(8, 9);
    this.kruskalSmall = new LightEmAll(2, 2, new Random(5), true);

    this.lemKruskal = new LightEmAll(5, 5, true);

    this.lem1Kruskal = new LightEmAll(3, 3, new Random(1), true);


    this.gp1 = new GamePiece(0, 0, false, true, false, false, true);
    this.gp2 = new GamePiece(0, 1, false, true, false, false, false);
    this.gp3 = new GamePiece(0, 2, false, true, false, false, false);
    ArrayList<GamePiece> row1 = new ArrayList<GamePiece>();
    row1.add(gp1);
    row1.add(gp2);
    row1.add(gp3);

    this.gp4 = new GamePiece(1, 0, true, true, false, true, false);
    this.gp5 = new GamePiece(1, 1, true, true, true, true, false);
    this.gp6 = new GamePiece(1, 2, true, true, true, false, false);
    ArrayList<GamePiece> row2 = new ArrayList<GamePiece>();
    row2.add(gp4);
    row2.add(gp5);
    row2.add(gp6);

    this.gp7 = new GamePiece(2, 0, true, false, false, false, false);
    this.gp8 = new GamePiece(2, 1, true, false, false, false, false);
    this.gp9 = new GamePiece(2, 2, true, false, false, false, false);
    ArrayList<GamePiece> row3 = new ArrayList<GamePiece>();
    row3.add(gp7);
    row3.add(gp8);
    row3.add(gp9);

    this.lem1Kruskal.board.set(0, row1);
    this.lem1Kruskal.board.set(1, row2);
    this.lem1Kruskal.board.set(2, row3);


    this.edge1 = new Edge(this.gp1, this.gp4, 2);
    this.edge2 = new Edge(this.gp4, this.gp7, 5);
    this.edge3 = new Edge(this.gp1, this.gp2, 2);
    this.edge4 = new Edge(this.gp4, this.gp5, 7);
    this.edge5 = new Edge(this.gp7, this.gp8, 9);
    this.edge6 = new Edge(this.gp2, this.gp5, 11);
    this.edge7 = new Edge(this.gp5, this.gp8, 11);
    this.edge8 = new Edge(this.gp2, this.gp3, 5); 
    this.edge9 = new Edge(this.gp5, this.gp6, 13);
    this.edge10 = new Edge(this.gp8, this.gp9, 15);
    this.edge11 = new Edge(this.gp3, this.gp6, 8);
    this.edge12 = new Edge(this.gp6, this.gp9, 20);

    sorted = new ArrayList<Edge>();
    sorted.add(this.edge1);
    sorted.add(this.edge3);
    sorted.add(this.edge2);
    sorted.add(this.edge8);
    sorted.add(this.edge4);
    sorted.add(this.edge11);
    sorted.add(this.edge5);
    sorted.add(this.edge6);
    sorted.add(this.edge7);
    sorted.add(this.edge9);
    sorted.add(this.edge10);
    sorted.add(this.edge12);

    total = new ArrayList<Edge>();
    for (Edge edge : lem1Kruskal.mst) {
      total.add(edge);
    }
  }

  //test for comparator, sorts weight from least to greatest 
  boolean testCompare(Tester t) {
    GamePiece gp = new GamePiece(1, 1);
    Edge edge1 = new Edge(gp, gp, 1); 
    Edge edge2 = new Edge(gp, gp, 2); 
    Edge edge3 = new Edge(gp, gp, 4); 
    ArrayList<Edge> edges = new ArrayList<Edge>(Arrays.asList(
        edge3, edge1, edge2));
    edges.sort(new CompareByWeight());

    return t.checkExpect(edges, new ArrayList<Edge>(Arrays.asList(
        edge1, edge2, edge3)));
  }

  //* Tests for UnionFind Class *//

  //test for find 
  boolean testFind(Tester t) {
    GamePiece gp3 = new GamePiece(5, 5);
    GamePiece gp1 = new GamePiece(1, 1);
    GamePiece gp2 = new GamePiece(2, 2);
    uf.union(gp1, gp2);

    //
    return t.checkExpect(uf.find(gp3), gp3)
        && t.checkExpect(uf.find(gp1), gp2);

  }

  //test for union
  void testUnion(Tester t) {
    GamePiece gp1 = new GamePiece(1, 1);
    GamePiece gp2 = new GamePiece(2, 2);
    this.uf.union(gp1, gp2);

    HashMap<GamePiece, GamePiece> hm = new HashMap<GamePiece, GamePiece>();
    hm.put(new GamePiece(2, 2), new GamePiece(1, 1));

    //check if unioned game pieces have the same root 
    t.checkExpect(this.uf.find(gp1), this.uf.find(gp2));
  }

  //test for isconnected 
  boolean testIsConnected(Tester t) {
    GamePiece gp3 = new GamePiece(5, 5);
    GamePiece gp1 = new GamePiece(1, 1);
    GamePiece gp2 = new GamePiece(2, 2);
    uf.union(gp1, gp2);

    return t.checkExpect(uf.isConnected(gp1, gp2), true)
        && t.checkExpect(uf.isConnected(gp1, gp3), false);
  }

  // * Tests for LightEmAll Class *//

  // test makeboard with kruskal 
  boolean testMakeBoard(Tester t) {
    this.initConditions();
    GamePiece gp1 = new GamePiece(0, 0, false, false, true, false, true);
    GamePiece gp2 = new GamePiece(1, 0, false, true, false, false, false);
    GamePiece gp3 = new GamePiece(0, 1, false, true, false, true, false);
    GamePiece gp4 = new GamePiece(1, 1, true, false, false, true, false);

    return t.checkExpect(this.kruskalSmall.board.get(0).get(0), gp1)
        && t.checkExpect(this.kruskalSmall.board.get(1).get(0), gp2)
        && t.checkExpect(this.kruskalSmall.board.get(0).get(1), gp3)
        && t.checkExpect(this.kruskalSmall.board.get(1).get(1), gp4);
  }

  // test makeBoard1 (original)
  boolean testMakeBoard1(Tester t) {
    this.initConditions();

    // test sameness of board on two different instances of a board
    return t.checkExpect(this.lem1.board, this.lem2.board)
        && t.checkExpect(this.lem1.board.get(0).get(0), this.lem2.board.get(0).get(0))
        && t.checkExpect(this.lem1.board.get(0).get(1), this.lem2.board.get(0).get(1))
        && t.checkExpect(this.lem1.board.get(0).get(2), this.lem2.board.get(0).get(2))
        && t.checkExpect(this.lem1.board.get(0).get(3), this.lem2.board.get(0).get(3))
        && t.checkExpect(this.lem1.board.get(1).get(0), this.lem2.board.get(1).get(0))
        && t.checkExpect(this.lem1.board.get(1).get(1), this.lem2.board.get(1).get(1))
        && t.checkExpect(this.lem1.board.get(1).get(2), this.lem2.board.get(1).get(2))
        && t.checkExpect(this.lem1.board.get(1).get(3), this.lem2.board.get(1).get(3))
        && t.checkExpect(this.lem1.board.get(2).get(0), this.lem2.board.get(2).get(0))
        && t.checkExpect(this.lem1.board.get(2).get(1), this.lem2.board.get(2).get(1))
        && t.checkExpect(this.lem1.board.get(2).get(2), this.lem2.board.get(2).get(2))
        && t.checkExpect(this.lem1.board.get(2).get(3), this.lem2.board.get(2).get(3))
        && t.checkExpect(this.lem1.board.get(3).get(0), this.lem2.board.get(3).get(0))
        && t.checkExpect(this.lem1.board.get(3).get(1), this.lem2.board.get(3).get(1))
        && t.checkExpect(this.lem1.board.get(3).get(2), this.lem2.board.get(3).get(2))
        && t.checkExpect(this.lem1.board.get(3).get(3), this.lem2.board.get(3).get(3))
        && t.checkExpect(this.lem1.board.get(4).get(0), this.lem2.board.get(4).get(0))
        && t.checkExpect(this.lem1.board.get(4).get(1), this.lem2.board.get(4).get(1))
        && t.checkExpect(this.lem1.board.get(4).get(2), this.lem2.board.get(4).get(2))
        && t.checkExpect(this.lem1.board.get(4).get(3), this.lem2.board.get(4).get(3));

  }

  // test onKeyEvent
  void testOnKeyEvent(Tester t) {
    this.initConditions();

    // check that the power station is in the middle
    t.checkExpect(this.lem3.board.get(4).get(4).powerStation, true);

    // press a key to move it up
    this.lem3.onKeyEvent("up");
    // check that power station is no longer there and is at new location
    t.checkExpect(this.lem3.board.get(4).get(4).powerStation, false);
    t.checkExpect(this.lem3.board.get(4).get(3).powerStation, true);

    // press wrong key
    this.lem3.onKeyEvent("a");
    // check that nothing happens
    t.checkExpect(this.lem3.board.get(4).get(4).powerStation, false);
    t.checkExpect(this.lem3.board.get(4).get(3).powerStation, true);

    // press a key to move it down
    this.lem3.onKeyEvent("down");
    // check that power station is no longer there and is at new location
    t.checkExpect(this.lem3.board.get(4).get(4).powerStation, true);
    t.checkExpect(this.lem3.board.get(3).get(4).powerStation, false);

    // press a key to move it left
    this.lem3.onKeyEvent("left");
    // check that power station is no longer there and is at new location
    t.checkExpect(this.lem3.board.get(4).get(4).powerStation, false);
    t.checkExpect(this.lem3.board.get(3).get(4).powerStation, true);

    // press a key to move it right
    this.lem3.onKeyEvent("right");
    // check that power station is no longer there and is at new location
    t.checkExpect(this.lem3.board.get(4).get(4).powerStation, true);
    t.checkExpect(this.lem3.board.get(3).get(4).powerStation, false);
  }

  // test worldEnds 
  boolean testWorldEnds(Tester t) {
    this.initConditions();

    TextImage winText = new TextImage("YOU WIN", this.lem4.size, Color.green);
    // initial conditions
    t.checkExpect(this.lem1.gameOver, false);

    // makes each game piece powered
    this.lem1.board.get(0).get(0).powered = true;
    this.lem1.board.get(0).get(1).powered = true;
    this.lem1.board.get(0).get(2).powered = true;
    this.lem1.board.get(0).get(3).powered = true;
    this.lem1.board.get(1).get(0).powered = true;
    this.lem1.board.get(1).get(1).powered = true;
    this.lem1.board.get(1).get(2).powered = true;
    this.lem1.board.get(1).get(3).powered = true;
    this.lem1.board.get(2).get(0).powered = true;
    this.lem1.board.get(2).get(1).powered = true;
    this.lem1.board.get(2).get(2).powered = true;
    this.lem1.board.get(2).get(3).powered = true;
    this.lem1.board.get(3).get(0).powered = true;
    this.lem1.board.get(3).get(1).powered = true;
    this.lem1.board.get(3).get(2).powered = true;
    this.lem1.board.get(3).get(3).powered = true;
    this.lem1.board.get(4).get(0).powered = true;
    this.lem1.board.get(4).get(1).powered = true;
    this.lem1.board.get(4).get(2).powered = true;
    this.lem1.board.get(4).get(3).powered = true;

    // making sure they're all turned the right way
    this.lem1.board.get(0).get(0).left = false;
    this.lem1.board.get(0).get(0).top = false;
    this.lem1.board.get(0).get(0).bottom = false;
    this.lem1.board.get(0).get(0).right = true;

    this.lem1.board.get(0).get(1).left = false;
    this.lem1.board.get(0).get(1).top = false;
    this.lem1.board.get(0).get(1).bottom = false;
    this.lem1.board.get(0).get(1).right = true;

    this.lem1.board.get(0).get(2).left = false;
    this.lem1.board.get(0).get(2).top = false;
    this.lem1.board.get(0).get(2).bottom = false;
    this.lem1.board.get(0).get(2).right = true;

    this.lem1.board.get(0).get(3).left = false;
    this.lem1.board.get(0).get(3).top = false;
    this.lem1.board.get(0).get(3).bottom = false;
    this.lem1.board.get(0).get(3).right = true;

    this.lem1.board.get(1).get(0).right = true;
    this.lem1.board.get(1).get(0).top = false;
    this.lem1.board.get(1).get(0).left = true;
    this.lem1.board.get(1).get(0).bottom = false;

    this.lem1.board.get(1).get(1).right = true;
    this.lem1.board.get(1).get(1).top = false;
    this.lem1.board.get(1).get(1).left = true;
    this.lem1.board.get(1).get(1).bottom = false;

    this.lem1.board.get(1).get(2).right = true;
    this.lem1.board.get(1).get(2).top = false;
    this.lem1.board.get(1).get(2).left = true;
    this.lem1.board.get(1).get(2).bottom = false;

    this.lem1.board.get(1).get(3).right = true;
    this.lem1.board.get(1).get(3).top = false;
    this.lem1.board.get(1).get(3).left = true;
    this.lem1.board.get(1).get(3).bottom = false;

    this.lem1.board.get(2).get(0).left = true;
    this.lem1.board.get(2).get(0).right = true;
    this.lem1.board.get(2).get(0).top = false;
    this.lem1.board.get(2).get(0).bottom = true;

    this.lem1.board.get(2).get(1).left = true;
    this.lem1.board.get(2).get(1).right = true;
    this.lem1.board.get(2).get(1).top = true;
    this.lem1.board.get(2).get(1).bottom = true;

    this.lem1.board.get(2).get(2).left = true;
    this.lem1.board.get(2).get(2).right = true;
    this.lem1.board.get(2).get(2).top = true;
    this.lem1.board.get(2).get(2).bottom = true;

    this.lem1.board.get(2).get(3).left = true;
    this.lem1.board.get(2).get(3).right = true;
    this.lem1.board.get(2).get(3).top = true;
    this.lem1.board.get(2).get(3).bottom = false;

    this.lem1.board.get(3).get(0).left = true;
    this.lem1.board.get(3).get(0).right = true;
    this.lem1.board.get(3).get(0).top = false;
    this.lem1.board.get(3).get(0).bottom = false;

    this.lem1.board.get(3).get(1).left = true;
    this.lem1.board.get(3).get(1).right = true;
    this.lem1.board.get(3).get(1).top = false;
    this.lem1.board.get(3).get(1).bottom = false;

    this.lem1.board.get(3).get(2).left = true;
    this.lem1.board.get(3).get(2).right = true;
    this.lem1.board.get(3).get(2).top = false;
    this.lem1.board.get(3).get(2).bottom = false;

    this.lem1.board.get(3).get(3).left = true;
    this.lem1.board.get(3).get(3).right = true;
    this.lem1.board.get(3).get(3).top = false;
    this.lem1.board.get(3).get(3).bottom = false;

    this.lem1.board.get(4).get(0).left = true;
    this.lem1.board.get(4).get(0).right = false;
    this.lem1.board.get(4).get(0).top = false;
    this.lem1.board.get(4).get(0).bottom = false;

    this.lem1.board.get(4).get(1).left = true;
    this.lem1.board.get(4).get(1).right = false;
    this.lem1.board.get(4).get(1).top = false;
    this.lem1.board.get(4).get(1).bottom = false;

    this.lem1.board.get(4).get(2).left = true;
    this.lem1.board.get(4).get(2).right = false;
    this.lem1.board.get(4).get(2).top = false;
    this.lem1.board.get(4).get(2).bottom = false;

    this.lem1.board.get(4).get(3).left = true;
    this.lem1.board.get(4).get(3).right = false;
    this.lem1.board.get(4).get(3).top = false;
    this.lem1.board.get(4).get(3).bottom = false;

    this.lem1.gameOver = true;

    WorldScene scene2 = this.lem1.makeScene();
    scene2.placeImageXY(winText, (this.lem1.width * this.lem1.size) / 2, 
        (this.lem1.height * this.lem1.size) / 2);
    WorldScene scene3 = this.lem2.makeScene();

    t.checkExpect(this.lem1.gameOver,true);

    // check that the world has ended and produced the winning text
    return t.checkExpect(this.lem1.worldEnds(), new WorldEnd(true, scene2))
        // check that the world has not ended

        // need to implement the random
        && t.checkExpect(this.lem2.worldEnds(), new WorldEnd(false, scene3));
  }

  // test bfs
  void testBFS(Tester t) {
    this.initConditions();


    // power station powered
    t.checkExpect(this.lem1.board.get(2).get(2).powered, true);

    // tile to the left of the power station is powered
    t.checkExpect(this.lem1.board.get(1).get(2).powered, true);

    // tile to the right of the power station is powered
    t.checkExpect(this.lem1.board.get(3).get(2).powered, true);

    // tile to the top of the power station is powered
    t.checkExpect(this.lem1.board.get(2).get(1).powered, true);

    // tile to the bottom of the power station is powered
    t.checkExpect(this.lem1.board.get(2).get(3).powered, true);

    // isn't powered and not next to the power station
    t.checkExpect(this.lem1.board.get(0).get(1).powered, false);

    // isn't powered and not next to the power station but next 
    // to a powered game piece
    t.checkExpect(this.lem1.board.get(3).get(3).powered, false);

  }


  // test searchHelp
  boolean testSearchHelp(Tester t) {
    this.initConditions();

    this.lem1.board.get(1).get(2).powered = true;
    t.checkExpect(this.lem1.searchHelp(2, 2, "left"), this.lem1.board.get(1).get(2));

    this.lem1.searchHelp(0, 2, "left");

    return t.checkExpect(this.lem1.searchHelp(0, 2, "left"), null);
  }

  // test onMouseClick
  void testOnMouseClicked(Tester t) {
    this.initConditions();
    GamePiece testPiece = this.lem1.board.get(0).get(0);

    t.checkExpect(testPiece.right, false);
    t.checkExpect(testPiece.left, false);
    t.checkExpect(testPiece.top, false);
    t.checkExpect(testPiece.bottom, true);
    this.lem1.onMouseClicked(new Posn(25, 25), "LeftButton");
    testPiece.rotateClockwise();
    t.checkExpect(this.lem1.board.get(0).get(0), testPiece);
  }

  // test makeScene
  boolean testMakeScene(Tester t) {
    this.initConditions();
    WorldScene lem = new WorldScene(200, 250);
    lem.placeImageXY(this.lem1.drawBoard(50), 125, 100);

    return t.checkExpect(this.lem1.makeScene(), lem);
  }


  //test drawBoard
  boolean testdrawBoard(Tester t) {
    this.initConditions();

    return t.checkExpect(this.lem1.drawBoard(this.lem1.size),
        new BesideImage(new BesideImage(new BesideImage(new BesideImage(
            new BesideImage(new EmptyImage(), 
                this.lem1.drawCol(this.lem1.board.get(0), this.lem1.size)),
            this.lem1.drawCol(this.lem1.board.get(1), this.lem1.size)),
            this.lem1.drawCol(this.lem1.board.get(2), this.lem1.size)),
            this.lem1.drawCol(this.lem1.board.get(3), this.lem1.size)),
            this.lem1.drawCol(this.lem1.board.get(4), this.lem1.size)));
  }

  // test drawCol
  boolean testDrawCol(Tester t) {
    this.initConditions();

    return t.checkExpect(this.lem1.drawCol(this.lem1.board.get(0),
        this.lem1.size), new AboveImage(new AboveImage(new AboveImage(
            new AboveImage(new EmptyImage(), 
                this.lem1.board.get(0).get(0).tileImage(this.lem1.size,
                    10, Color.LIGHT_GRAY, false)),
            this.lem1.board.get(0).get(1).tileImage(this.lem1.size,
                10, Color.LIGHT_GRAY, false)),
            this.lem1.board.get(0).get(2).tileImage(this.lem1.size,
                10, Color.yellow, false)),
            this.lem1.board.get(0).get(3).tileImage(this.lem1.size,
                10, Color.LIGHT_GRAY, false)))
        && t.checkExpect(this.lem1.drawCol(this.lem1.board.get(2),
            this.lem1.size), new AboveImage(new AboveImage(new AboveImage(
                new AboveImage(new EmptyImage(), 
                    this.lem1.board.get(2).get(0).tileImage(this.lem1.size,
                        10, Color.yellow, false)),
                this.lem1.board.get(2).get(1).tileImage(this.lem1.size,
                    10, Color.yellow, false)),
                this.lem1.board.get(2).get(2).tileImage(this.lem1.size,
                    10, Color.yellow, true)),
                this.lem1.board.get(2).get(3).tileImage(this.lem1.size,
                    10, Color.yellow, false)));
  }

  // test randomize (rotate gamepieces random amount of times)
  void testRandomize(Tester t)  {
    this.initConditions();

    t.checkExpect(this.lem1.board.get(0).get(0).top, false);
    t.checkExpect(this.lem1.board.get(0).get(0).bottom, true);
    t.checkExpect(this.lem1.board.get(0).get(0).left, false);
    t.checkExpect(this.lem1.board.get(0).get(0).right, false);

    this.lem1.randomize(new Random(1));

    t.checkExpect(this.lem1.board.get(0).get(0).top, false);
    t.checkExpect(this.lem1.board.get(0).get(0).bottom, false);
    t.checkExpect(this.lem1.board.get(0).get(0).left, true);
    t.checkExpect(this.lem1.board.get(0).get(0).right, false);

  }

  //test isGameOver
  void testIsGameOver(Tester t) {
    this.initConditions();

    // initially game is not over
    t.checkExpect(this.lem4.gameOver, false);

    // makes each game piece powered
    this.lem4.board.get(0).get(0).powered = true;
    this.lem4.board.get(0).get(1).powered = true;
    this.lem4.board.get(0).get(2).powered = true;
    this.lem4.board.get(0).get(3).powered = true;
    this.lem4.board.get(1).get(0).powered = true;
    this.lem4.board.get(1).get(1).powered = true;
    this.lem4.board.get(1).get(2).powered = true;
    this.lem4.board.get(1).get(3).powered = true;
    this.lem4.board.get(2).get(0).powered = true;
    this.lem4.board.get(2).get(1).powered = true;
    this.lem4.board.get(2).get(2).powered = true;
    this.lem4.board.get(2).get(3).powered = true;
    this.lem4.board.get(3).get(0).powered = true;
    this.lem4.board.get(3).get(1).powered = true;
    this.lem4.board.get(3).get(2).powered = true;
    this.lem4.board.get(3).get(3).powered = true;
    this.lem4.board.get(4).get(0).powered = true;
    this.lem4.board.get(4).get(1).powered = true;
    this.lem4.board.get(4).get(2).powered = true;
    this.lem4.board.get(4).get(3).powered = true;

    // sets gameOver boolean 
    this.lem4.isGameOver();

    // game is over
    t.checkExpect(this.lem4.gameOver, true);
  }

  //test for generateNodes 
  void testGenerateNodes(Tester t) {
    this.initConditions();
    GamePiece gp1 = this.kruskalSmall.board.get(0).get(0);
    GamePiece gp2 = this.kruskalSmall.board.get(0).get(1);
    GamePiece gp3 = this.kruskalSmall.board.get(1).get(0);
    GamePiece gp4 = this.kruskalSmall.board.get(1).get(1);

    //check if nodes list was set properly 
    t.checkExpect(this.kruskalSmall.nodes, new ArrayList<GamePiece>(
        Arrays.asList(gp1, gp2, gp3, gp4)));
  }

  //test for generateEdges with random weights 
  void testGenerateEdges(Tester t) {   
    //gamepieces 
    this.initConditions();
    GamePiece gp1 = this.kruskalSmall.board.get(0).get(0);
    GamePiece gp2 = this.kruskalSmall.board.get(1).get(0);
    GamePiece gp3 = this.kruskalSmall.board.get(1).get(1);
    GamePiece gp4 = this.kruskalSmall.board.get(0).get(1);

    Edge edge1 = new Edge(gp3, gp4, 74);
    Edge edge2 = new Edge(gp3, gp2, 24); 
    Edge edge3 = new Edge(gp4, gp1, 87);

    t.checkExpect(this.kruskalSmall.mst.get(0), edge2);
    t.checkExpect(this.kruskalSmall.mst.get(1), edge1);
    t.checkExpect(this.kruskalSmall.mst.get(2), edge3);
  }

  //test for connectedges
  void testconnectEdges(Tester t) {
    this.initConditions();

    //check if the edge's to and from has the right connections 
    Edge edge1 = this.kruskalSmall.mst.get(0);
    t.checkExpect(edge1.from.left, true);
    t.checkExpect(edge1.from.bottom, true);
    t.checkExpect(edge1.to.left, false);
    t.checkExpect(edge1.to.right, true);
    t.checkExpect(edge1.to.top, false);
    t.checkExpect(edge1.to.bottom, false);

  }

  //test kruskalmst 
  boolean testKruskal(Tester t) {
    this.initConditions();

    // Check if all edges in sorted are present in mst
    boolean allEdgesPresent = true;
    for (Edge edge : sorted) {
      if (!lem1Kruskal.mst.contains(edge)) {
        allEdgesPresent = false;
      }
    }

    boolean allThere = true;
    for (Edge edge : total) {
      if (!lem1Kruskal.mst.contains(edge)) {
        allThere = false;
      }
    }


    // test where all edges in list don't match the edges in mst
    return t.checkExpect(allEdgesPresent, false)
        // test where mst is an empty list
        && t.checkExpect(this.lem5.mst, new ArrayList<Edge>())
        // test where all the edges in list match the edges in mst
        && t.checkExpect(allThere, true);

  }



  // * Tests for Game Piece Class *//

  // test movePower
  void testMovePower(Tester t) {
    this.initConditions();

    // on a game piece that is power station
    t.checkExpect(this.lem3.board.get(4).get(4).powerStation, true);
    t.checkExpect(this.lem3.board.get(4).get(4).powered, true);

    // call on movePower
    this.lem3.board.get(4).get(4).movePower();

    // make it a regular game piece
    t.checkExpect(this.lem3.board.get(4).get(4).powerStation, false);
    t.checkExpect(this.lem3.board.get(4).get(4).powered, false);

    // not power station
    t.checkExpect(this.lem3.board.get(3).get(4).powerStation, false);
    // t.checkExpect(this.lem3.board.get(3).get(4).powered, false);

    // call on movePower
    this.lem3.board.get(3).get(4).movePower();

    // make it a powerStation
    t.checkExpect(this.lem3.board.get(3).get(4).powerStation, true);
    t.checkExpect(this.lem3.board.get(3).get(4).powered, true);


  }

  // test hasTop
  boolean testHasTop(Tester t) {
    this.initConditions();
    // test on a game piece that doesn't have a top
    return t.checkExpect(this.lem2.board.get(0).get(0).hasTop(), false)
        // test on a game piece that has a top
        && t.checkExpect(this.lem1.board.get(2).get(2).hasTop(), true);
  }

  //test hasBot
  boolean testHasBot(Tester t) {
    this.initConditions();
    // test on a game piece that doesn't have a bottom
    return t.checkExpect(this.lem1.board.get(0).get(3).hasBot(), false)
        // test on a game piece that has a bottom
        && t.checkExpect(this.lem1.board.get(2).get(1).hasBot(), true);

  }

  //test hasLeft
  boolean testHasLeft(Tester t) {
    this.initConditions();
    // test on a game piece that doesn't have a left
    return t.checkExpect(this.lem1.board.get(0).get(0).hasLeft(), false)
        // test on a game piece that has a left
        && t.checkExpect(this.lem1.board.get(4).get(0).hasLeft(), true);

  }

  //test hasRight
  boolean testHasRight(Tester t) {
    this.initConditions();
    // test on a game piece that doesn't have a right
    return t.checkExpect(this.lem1.board.get(4).get(0).hasRight(), false)
        // test on a game piece that has a right
        && t.checkExpect(this.lem1.board.get(2).get(0).hasRight(), true);
  }

  // test isPowered
  boolean testIsPowered(Tester t) {
    this.initConditions();

    this.lem2.board.get(0).get(0).powered = true;

    return t.checkExpect(this.lem1.board.get(0).get(0).isPowered(), false)
        && t.checkExpect(this.lem2.board.get(0).get(0).isPowered(), true);
  }

  //test for rotateCLockwise 
  void testRotateClockwise(Tester t) {
    this.initConditions();
    GamePiece testPiece = this.lem1.board.get(0).get(0);

    t.checkExpect(testPiece.right, false);
    t.checkExpect(testPiece.left, false);
    t.checkExpect(testPiece.top, false);
    t.checkExpect(testPiece.bottom, true);
    testPiece.rotateClockwise();
    t.checkExpect(testPiece.right, false);
    t.checkExpect(testPiece.left, true);
    t.checkExpect(testPiece.top, false);
    t.checkExpect(testPiece.bottom, false);
  }

  // test randGP
  void testRandGP(Tester t) {
    this.initConditions();

    GamePiece g = new GamePiece(0, 3);

    // values before calling on the random
    t.checkExpect(g.top, false);
    t.checkExpect(g.bottom, false);
    t.checkExpect(g.left, false);
    t.checkExpect(g.right, false);

    g.randGP(new Random(1));

    // checking that values have changed after
    t.checkExpect(g.top, true);
    t.checkExpect(g.bottom, false);
    t.checkExpect(g.left, false);
    t.checkExpect(g.right, false);

  }

  /*
  //game test (without kruskal)
  void testBigBang(Tester t) {
    this.initConditions();

    int width = this.lem1.width * this.lem1.size;
    int height = this.lem1.height * this.lem1.size;
    double tickRate = 1.0;


    this.lem1.bigBang(width, height, tickRate);
  }
   */

  /*
  //small kruskal board for testing 
  void testBigBang(Tester t) {
    this.initConditions();

    int width = this.kruskalSmall.width * this.kruskalSmall.size;
    int height = this.kruskalSmall.height * this.kruskalSmall.size;
    double tickRate = 1.0;

    this.kruskalSmall.bigBang(width, height, tickRate);
  }
   */


  //actual random game using kruskal
  void testBigBang(Tester t) {
    this.initConditions();

    int width = this.lemKruskal.width * this.lemKruskal.size;
    int height = this.lemKruskal.height * this.lemKruskal.size;
    double tickRate = 1.0;

    this.lemKruskal.bigBang(width, height, tickRate);
  }

}